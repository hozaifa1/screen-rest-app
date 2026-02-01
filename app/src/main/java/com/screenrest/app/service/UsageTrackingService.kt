package com.screenrest.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.screenrest.app.MainActivity
import com.screenrest.app.R
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.TrackingMode
import com.screenrest.app.receiver.BlockCompleteReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class UsageTrackingService : LifecycleService() {
    
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var usageCalculator: UsageCalculator
    @Inject lateinit var permissionChecker: PermissionChecker
    
    private var trackingJob: Job? = null
    private var trackingStartTimestamp: Long = 0L
    private var lastDayCheck: Int = -1
    private var cumulativeUsageToday: Long = 0L
    
    private val blockCompleteReceiver = BlockCompleteReceiver()
    
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerBlockCompleteReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val notification = createNotification("Monitoring screen time...")
        startForeground(NOTIFICATION_ID, notification)
        
        startTracking()
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        unregisterReceiver(blockCompleteReceiver)
    }
    
    private fun registerBlockCompleteReceiver() {
        val filter = IntentFilter("com.screenrest.app.ACTION_BLOCK_COMPLETE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(blockCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(blockCompleteReceiver, filter)
        }
    }
    
    private fun startTracking() {
        trackingStartTimestamp = System.currentTimeMillis()
        lastDayCheck = getCurrentDay()
        
        trackingJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    performTrackingCycle()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    private suspend fun performTrackingCycle() {
        if (!permissionChecker.checkUsageStatsPermission()) {
            updateNotification("Missing usage stats permission")
            return
        }
        
        val breakConfig = settingsRepository.breakConfig.first()
        val currentUsageMs = calculateCurrentUsage(breakConfig)
        val currentUsageMinutes = (currentUsageMs / 60_000).toInt()
        
        checkDailyReset(breakConfig)
        
        val thresholdReached = currentUsageMinutes >= breakConfig.usageThresholdMinutes
        
        if (thresholdReached) {
            if (breakConfig.locationEnabled) {
                if (isInTargetLocation(breakConfig)) {
                    triggerBlock(breakConfig)
                } else {
                    updateNotification("Outside target location - ${formatTime(currentUsageMs)}")
                }
            } else {
                triggerBlock(breakConfig)
            }
        } else {
            val remaining = breakConfig.usageThresholdMinutes - currentUsageMinutes
            updateNotification("Time used: ${formatTime(currentUsageMs)} | ${remaining}m until break")
        }
    }
    
    private suspend fun calculateCurrentUsage(breakConfig: BreakConfig): Long {
        return when (breakConfig.trackingMode) {
            TrackingMode.CONTINUOUS -> {
                usageCalculator.getContinuousUsageSinceTimestamp(trackingStartTimestamp)
            }
            TrackingMode.CUMULATIVE -> {
                cumulativeUsageToday
            }
        }
    }
    
    private fun checkDailyReset(breakConfig: BreakConfig) {
        val currentDay = getCurrentDay()
        if (currentDay != lastDayCheck && breakConfig.trackingMode == TrackingMode.CUMULATIVE) {
            cumulativeUsageToday = 0L
            lastDayCheck = currentDay
        }
    }
    
    private fun getCurrentDay(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.DAY_OF_YEAR)
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
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun triggerBlock(breakConfig: BreakConfig) {
        BlockAccessibilityService.isBlockActive = true
        
        val intent = Intent(this, Class.forName("com.screenrest.app.presentation.block.BlockActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("BLOCK_DURATION_SECONDS", breakConfig.blockDurationSeconds)
        }
        startActivity(intent)
        
        when (breakConfig.trackingMode) {
            TrackingMode.CONTINUOUS -> {
                trackingStartTimestamp = System.currentTimeMillis()
            }
            TrackingMode.CUMULATIVE -> {
                cumulativeUsageToday = 0L
            }
        }
    }
    
    private fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 60_000).toInt()
        val seconds = ((milliseconds % 60_000) / 1000).toInt()
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
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screenrest_tracking"
        private const val POLLING_INTERVAL_MS = 30_000L
    }
}
