package com.screenrest.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.screenrest.app.MainActivity
import com.screenrest.app.R
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.presentation.block.BlockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class UsageTrackingService : LifecycleService() {
    
    companion object {
        private const val TAG = "UsageTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screenrest_tracking"
        private const val POLLING_INTERVAL_MS = 1_000L
        
        @Volatile
        var currentUsageMs: Long = 0L
            private set
        
        @Volatile
        var thresholdMs: Long = 0L
            private set
        
        @Volatile
        private var hasTriggeredBlockThisCycle = false
    }
    
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var permissionChecker: PermissionChecker
    
    private var trackingJob: Job? = null
    private var configJob: Job? = null
    private var whitelistJob: Job? = null
    private var accumulatedUsageMs: Long = 0L
    private var lastScreenOnTimestamp: Long = 0L
    private var isScreenOn: Boolean = true
    private var isInWhitelistApp: Boolean = false
    private var whitelistPauseStartTime: Long = 0L
    
    private var cachedBreakConfig: BreakConfig = BreakConfig()
    private var whitelistApps: Set<String> = emptySet()
    
    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    
    private val usageStatsManager by lazy {
        getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    lastScreenOnTimestamp = System.currentTimeMillis()
                    Log.d(TAG, "Screen ON, starting new session")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    val now = System.currentTimeMillis()
                    val sessionTime = now - lastScreenOnTimestamp
                    accumulatedUsageMs += sessionTime
                    isScreenOn = false
                    Log.d(TAG, "Screen OFF, session=${sessionTime}ms, total=${accumulatedUsageMs}ms")
                }
            }
        }
    }
    
    private val blockCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.screenrest.app.ACTION_BLOCK_COMPLETE") {
                Log.d(TAG, "Block complete, resetting all usage tracking")
                resetAfterBlock()
                lifecycleScope.launch {
                    settingsRepository.updateLastBreakTimestamp(System.currentTimeMillis())
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceivers()
        
        isScreenOn = powerManager.isInteractive
        lastScreenOnTimestamp = System.currentTimeMillis()
        accumulatedUsageMs = 0L
        
        Log.d(TAG, "Service onCreate: isScreenOn=$isScreenOn")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val notification = createNotification("Monitoring screen time...")
        startForeground(NOTIFICATION_ID, notification)
        
        // Start config collector
        configJob?.cancel()
        configJob = lifecycleScope.launch {
            settingsRepository.breakConfig.collect { config ->
                cachedBreakConfig = config
                thresholdMs = config.usageThresholdSeconds * 1_000L
                Log.d(TAG, "Config updated: threshold=${config.usageThresholdSeconds}s")
            }
        }
        
        // Start whitelist apps collector
        whitelistJob?.cancel()
        whitelistJob = lifecycleScope.launch {
            try {
                settingsRepository.whitelistApps.collect { apps ->
                    whitelistApps = apps
                    Log.d(TAG, "Whitelist apps loaded: ${apps.size} apps -> $apps")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting whitelist apps", e)
            }
        }
        
        lifecycleScope.launch {
            startTracking()
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        configJob?.cancel()
        try {
            unregisterReceiver(screenReceiver)
            unregisterReceiver(blockCompleteReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers", e)
        }
    }
    
    private fun registerReceivers() {
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenFilter)
        
        val blockFilter = IntentFilter("com.screenrest.app.ACTION_BLOCK_COMPLETE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(blockCompleteReceiver, blockFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(blockCompleteReceiver, blockFilter)
        }
    }
    
    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    performTrackingCycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in tracking cycle", e)
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    private suspend fun performTrackingCycle() {
        val breakConfig = cachedBreakConfig
        
        // Detect when block overlay has finished - don't rely only on broadcast
        if (hasTriggeredBlockThisCycle && !BlockOverlayService.isOverlayActive) {
            Log.d(TAG, "Block cycle ended (overlay inactive), resetting tracking")
            resetAfterBlock()
        }
        
        // Check if user is in a whitelisted app
        checkWhitelistStatus()
        
        currentUsageMs = calculateCurrentUsage()
        
        val thresholdReached = currentUsageMs >= thresholdMs
        
        if (thresholdReached && !isInWhitelistApp && !hasTriggeredBlockThisCycle && !BlockAccessibilityService.isBlockActive && !BlockOverlayService.isOverlayActive && isScreenOn) {
            Log.w(TAG, "THRESHOLD REACHED! Usage=${currentUsageMs}ms >= ${thresholdMs}ms")
            hasTriggeredBlockThisCycle = true
            
            if (breakConfig.locationEnabled) {
                val inLocation = isInTargetLocation(breakConfig)
                if (inLocation) {
                    attemptBlockLaunch(breakConfig)
                } else {
                    Log.d(TAG, "Block skipped - outside target location")
                    updateNotification("Outside target location - ${formatTime(currentUsageMs)}")
                    hasTriggeredBlockThisCycle = false
                }
            } else {
                attemptBlockLaunch(breakConfig)
            }
        } else if (thresholdReached && (hasTriggeredBlockThisCycle || BlockAccessibilityService.isBlockActive || BlockOverlayService.isOverlayActive)) {
            updateNotification("Block screen active - please wait")
        } else if (!thresholdReached) {
            val remainingMs = thresholdMs - currentUsageMs
            val remainingMinutes = (remainingMs / 60_000).toInt()
            val remainingSeconds = ((remainingMs % 60_000) / 1000).toInt()
            val statusMsg = if (isInWhitelistApp) {
                "Timer paused (whitelist app) | Break in ${remainingMinutes}m ${remainingSeconds}s"
            } else {
                "Used: ${formatTime(currentUsageMs)} | Break in ${remainingMinutes}m ${remainingSeconds}s"
            }
            updateNotification(statusMsg)
        }
    }
    
    private fun attemptBlockLaunch(breakConfig: BreakConfig) {
        if (android.provider.Settings.canDrawOverlays(this)) {
            Log.w(TAG, "✅ TRIGGERING BLOCK (Overlay permission granted)")
            triggerBlock(breakConfig)
        } else {
            Log.w(TAG, "⚠️ Overlay permission MISSING. Showing notification instead.")
            showBlockNotification(breakConfig)
        }
    }

    private fun showBlockNotification(breakConfig: BreakConfig) {
        // Fallback for when we can't launch activity directly
        val intent = Intent(this, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra("BLOCK_DURATION_SECONDS", breakConfig.blockDurationSeconds)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            1, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TIME FOR A BREAK!")
            .setContentText("Tap immediately to start your break.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // This might popup if permitted
            .setAutoCancel(true)
            .build()
            
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
        
        // We still mark block as active to prevent spam
        BlockAccessibilityService.isBlockActive = true
    }
    
    private fun checkWhitelistStatus() {
        if (whitelistApps.isEmpty()) {
            if (isInWhitelistApp) {
                isInWhitelistApp = false
                whitelistPauseStartTime = 0L
            }
            return
        }
        
        val currentApp = getCurrentForegroundApp()
        
        // Exclude our own package from detection
        val ownPackage = applicationContext.packageName
        val effectiveApp = if (currentApp == ownPackage) null else currentApp
        
        val shouldBePaused = effectiveApp != null && whitelistApps.contains(effectiveApp)
        
        if (shouldBePaused && !isInWhitelistApp) {
            // Just entered whitelist app - pause tracking
            isInWhitelistApp = true
            whitelistPauseStartTime = System.currentTimeMillis()
            Log.d(TAG, "Entered whitelist app: $effectiveApp - pausing timer")
        } else if (!shouldBePaused && isInWhitelistApp) {
            // Just left whitelist app - resume tracking
            val pauseDuration = System.currentTimeMillis() - whitelistPauseStartTime
            lastScreenOnTimestamp += pauseDuration
            isInWhitelistApp = false
            whitelistPauseStartTime = 0L
            Log.d(TAG, "Left whitelist app - resuming timer (paused for ${pauseDuration}ms)")
        }
    }
    
    private fun getCurrentForegroundApp(): String? {
        val manager = usageStatsManager ?: run {
            Log.e(TAG, "UsageStatsManager is null")
            return null
        }
        
        return try {
            val now = System.currentTimeMillis()
            
            // Approach 1: Use UsageEvents (most accurate)
            val events = manager.queryEvents(now - 10_000, now)
            var foregroundApp: String? = null
            var latestTimestamp = 0L
            
            if (events != null) {
                val event = android.app.usage.UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val type = event.eventType
                    // MOVE_TO_FOREGROUND = 1 (all versions), ACTIVITY_RESUMED = 15 (API 29+)
                    if (type == 1 || type == 15) {
                        if (event.timeStamp >= latestTimestamp) {
                            latestTimestamp = event.timeStamp
                            foregroundApp = event.packageName
                        }
                    }
                }
            }
            
            // Approach 2: Fallback to queryUsageStats if events didn't work
            if (foregroundApp == null) {
                val stats = manager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    now - 60_000,
                    now
                )
                foregroundApp = stats
                    ?.filter { it.lastTimeUsed > 0 && it.totalTimeInForeground > 0 }
                    ?.maxByOrNull { it.lastTimeUsed }
                    ?.packageName
                    
                if (foregroundApp != null) {
                    Log.d(TAG, "Foreground app (fallback): $foregroundApp")
                }
            } else {
                Log.d(TAG, "Foreground app (events): $foregroundApp")
            }
            
            foregroundApp
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            null
        }
    }
    
    private fun calculateCurrentUsage(): Long {
        val currentSessionTime = if (isScreenOn) {
            if (isInWhitelistApp && whitelistPauseStartTime > 0) {
                // Freeze timer at the moment user entered the whitelist app
                whitelistPauseStartTime - lastScreenOnTimestamp
            } else {
                System.currentTimeMillis() - lastScreenOnTimestamp
            }
        } else {
            0L
        }
        
        return accumulatedUsageMs + maxOf(0L, currentSessionTime)
    }
    
    private fun resetAfterBlock() {
        accumulatedUsageMs = 0L
        lastScreenOnTimestamp = System.currentTimeMillis()
        currentUsageMs = 0L
        hasTriggeredBlockThisCycle = false
        BlockAccessibilityService.isBlockActive = false
        Log.d(TAG, "Tracking reset after block complete")
    }
    
    private suspend fun isInTargetLocation(breakConfig: BreakConfig): Boolean {
        if (!permissionChecker.checkLocationPermission()) return false
        if (breakConfig.locationLat == null || breakConfig.locationLng == null) return false
        
        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val targetLocation = Location("").apply {
                    latitude = breakConfig.locationLat
                    longitude = breakConfig.locationLng
                }
                val distance = location.distanceTo(targetLocation)
                distance <= breakConfig.locationRadiusMeters
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            true
        }
    }
    
    private fun triggerBlock(breakConfig: BreakConfig) {
        Log.w(TAG, "TRIGGERING BLOCK: duration=${breakConfig.blockDurationSeconds}s")
        
        try {
            BlockAccessibilityService.isBlockActive = true
            
            val overlayIntent = Intent(this, BlockOverlayService::class.java).apply {
                putExtra(BlockOverlayService.EXTRA_DURATION_SECONDS, breakConfig.blockDurationSeconds)
            }
            startService(overlayIntent)
            Log.d(TAG, "BlockOverlayService started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start block: ${e.message}", e)
            BlockAccessibilityService.isBlockActive = false
            hasTriggeredBlockThisCycle = false
        }
    }
    
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    private fun createNotification(message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenRest Active")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your screen time usage"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
