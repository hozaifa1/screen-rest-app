package com.screenrest.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.view.MotionEvent
import android.view.View
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.screenrest.app.util.DeviceAdminHelper
import com.screenrest.app.data.local.datastore.MessageIndexDataStore
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.DisplayMessage
import com.screenrest.app.domain.model.ThemeColor
import com.screenrest.app.domain.model.ThemeMode
import com.screenrest.app.domain.usecase.GetRandomDisplayMessageUseCase
import com.screenrest.app.presentation.theme.ScreenRestTheme
import com.screenrest.app.presentation.theme.getThemeColorPalette
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BlockOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "BlockOverlayService"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val EXTRA_BLOCK_MODE = "block_mode"
        const val EXTRA_CUSTOM_MESSAGE = "custom_message"
        const val EXTRA_END_TIME_MS = "end_time_ms"

        const val MODE_USAGE = "usage"
        const val MODE_SCHEDULED = "scheduled"

        @Volatile
        var isOverlayActive = false
            private set

        /** True while an incoming or active call is in progress. Read by BlockAccessibilityService. */
        @Volatile
        var isCallActive = false
    }

    @Inject
    lateinit var getRandomDisplayMessageUseCase: GetRandomDisplayMessageUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var messageIndexDataStore: MessageIndexDataStore

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var statusBarGuardView: android.view.View? = null
    private val statusBarHandler = Handler(Looper.getMainLooper())
    private val permissionWatchdogHandler = Handler(Looper.getMainLooper())
    private val phoneModeTimeoutHandler = Handler(Looper.getMainLooper())
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var countdownJob: Job? = null
    private var remainingSeconds by mutableIntStateOf(30)
    private var currentDisplayMessage by mutableStateOf<DisplayMessage?>(null)
    private var currentThemeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var currentThemeColor by mutableStateOf(ThemeColor.TEAL)
    private var originalThemeColor: ThemeColor = ThemeColor.TEAL

    // Feature states — observed by the Compose content lambda
    private var showTimerCountdown by mutableStateOf(true)
    private var hasBeenExtended by mutableStateOf(false)

    // Phone mode — not Compose state, managed imperatively
    private var isPhoneModeActive = false

    private var currentBlockDurationSeconds: Int = -1
    private var isCountdownRunning = false
    private var isScreenOn = true

    private var currentBlockMode: String = MODE_USAGE
    private var scheduledEndTimeMs: Long = 0L
    private var customMessageText: String? = null

    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    // ── Screen on/off receiver ────────────────────────────────────────────────

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    Log.d(TAG, "Screen ON during block - countdown will resume")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    Log.d(TAG, "Screen OFF during block - countdown paused")
                }
            }
        }
    }

    // ── Phone state receiver ─────────────────────────────────────────────────

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                TelephonyManager.EXTRA_STATE_RINGING,
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    isCallActive = true
                    // Cancel the auto-exit timeout since a real call is in progress
                    phoneModeTimeoutHandler.removeCallbacks(phoneModeTimeoutRunnable)
                    if (!isPhoneModeActive) enterPhoneMode()
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Brief delay so the call UI can close gracefully before overlay returns
                    Handler(Looper.getMainLooper()).postDelayed({
                        isCallActive = false
                        if (isPhoneModeActive) exitPhoneMode()
                    }, 1500L)
                }
            }
        }
    }

    /** Auto-exit phone mode after 5 minutes if no call was placed. */
    private val phoneModeTimeoutRunnable = Runnable {
        Log.d(TAG, "Phone mode timeout — returning to block screen")
        exitPhoneMode()
    }

    // ── Lifecycle boilerplate ────────────────────────────────────────────────

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        isScreenOn = powerManager.isInteractive

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(screenReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, screenFilter)
        }

        // Register phone-state receiver for call detection (gracefully skipped if unavailable)
        try {
            val phoneFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerReceiver(phoneStateReceiver, phoneFilter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(phoneStateReceiver, phoneFilter)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Phone state receiver not registered: ${e.message}")
        }

        Log.d(TAG, "BlockOverlayService created, screenOn=$isScreenOn")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 30) ?: 30
        val blockMode = intent?.getStringExtra(EXTRA_BLOCK_MODE) ?: MODE_USAGE
        val endTimeMs = intent?.getLongExtra(EXTRA_END_TIME_MS, 0L) ?: 0L
        val incomingCustomMessage = intent?.getStringExtra(EXTRA_CUSTOM_MESSAGE)

        Log.d(TAG, "Starting overlay with duration=$duration mode=$blockMode endTimeMs=$endTimeMs")

        isOverlayActive = true
        currentBlockMode = blockMode
        scheduledEndTimeMs = endTimeMs
        customMessageText = incomingCustomMessage

        lifecycleScope.launch {
            if (currentBlockDurationSeconds != duration || !isCountdownRunning) {
                currentBlockDurationSeconds = duration
                remainingSeconds = duration
                hasBeenExtended = false

                // Exit phone mode if a previous block left it active
                if (isPhoneModeActive) exitPhoneMode()

                // Load theme settings
                try {
                    currentThemeMode = settingsRepository.themeMode.first()
                    originalThemeColor = settingsRepository.themeColor.first()
                    currentThemeColor = getNextThemeColor()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading theme", e)
                }

                // Load timer-visibility setting
                try {
                    showTimerCountdown = settingsRepository.showTimerCountdown.first()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading timer visibility setting", e)
                }

                // Load display message
                try {
                    currentDisplayMessage = if (blockMode == MODE_SCHEDULED && incomingCustomMessage != null) {
                        DisplayMessage.IslamicReminder(incomingCustomMessage)
                    } else {
                        getRandomDisplayMessageUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading message", e)
                    currentDisplayMessage = null
                }

                if (overlayView == null) showOverlay()

                delay(200)
                startCountdown(duration)
            } else {
                Log.d(TAG, "Block already running with same duration, continuing...")
            }
        }

        return START_NOT_STICKY
    }

    // ── Overlay management ───────────────────────────────────────────────────

    private fun showOverlay() {
        try {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Log.e(TAG, "Cannot draw overlays - permission not granted")
                stopSelf()
                return
            }

            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@BlockOverlayService)
                setViewTreeViewModelStoreOwner(this@BlockOverlayService)
                setViewTreeSavedStateRegistryOwner(this@BlockOverlayService)

                setContent {
                    ScreenRestTheme(themeMode = currentThemeMode, themeColor = currentThemeColor) {
                        BlockOverlayContent(
                            remainingSeconds = remainingSeconds,
                            displayMessage = currentDisplayMessage,
                            isDarkTheme = when (currentThemeMode) {
                                ThemeMode.DARK -> true
                                ThemeMode.LIGHT -> false
                                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                            },
                            themeColor = currentThemeColor,
                            isScheduledBlock = currentBlockMode == MODE_SCHEDULED,
                            showTimerCountdown = showTimerCountdown,
                            hasBeenExtended = hasBeenExtended,
                            onExtendTime = { extendTimeByMinutes(30) },
                            onNextMessage = { loadNextMessage() },
                            onOpenPhone = { handleOpenPhoneButton() }
                        )
                    }
                }
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            overlayParams = params
            windowManager?.addView(overlayView, params)

            addStatusBarGuard()
            startCollapsingPanels()
            startPermissionWatchdog()

            isOverlayActive = true
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED

            Log.d(TAG, "Overlay view added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
            stopSelf()
        }
    }

    private fun addStatusBarGuard() {
        try {
            val guard = object : View(this) {
                override fun onTouchEvent(event: MotionEvent?): Boolean = true
            }
            guard.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            val guardParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                300,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = -100
            }

            windowManager?.addView(guard, guardParams)
            statusBarGuardView = guard
            Log.d(TAG, "Status bar guard added")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding status bar guard", e)
        }
    }

    // ── Notification shade blocking ──────────────────────────────────────────

    private val collapseRunnable = object : Runnable {
        override fun run() {
            if (isOverlayActive) {
                collapsePanels()
                statusBarHandler.postDelayed(this, 100)
            }
        }
    }

    private fun startCollapsingPanels() {
        statusBarHandler.post(collapseRunnable)
    }

    private fun stopCollapsingPanels() {
        statusBarHandler.removeCallbacks(collapseRunnable)
    }

    private fun collapsePanels() {
        try {
            @Suppress("WrongConstant")
            val statusBarService = getSystemService("statusbar")
            statusBarService?.javaClass?.getMethod("collapsePanels")?.invoke(statusBarService)
        } catch (e: Exception) {
            // Expected on some devices / Android versions
        }
    }

    // ── Permission watchdog ──────────────────────────────────────────────────

    private val permissionWatchdogRunnable = object : Runnable {
        override fun run() {
            if (isOverlayActive && isCountdownRunning && !isPhoneModeActive) {
                if (!android.provider.Settings.canDrawOverlays(this@BlockOverlayService)) {
                    Log.w(TAG, "Overlay permission REVOKED during active block — locking device")
                    DeviceAdminHelper.forceLockScreen(this@BlockOverlayService)
                }
            }
            permissionWatchdogHandler.postDelayed(this, 1_000L)
        }
    }

    private fun startPermissionWatchdog() {
        permissionWatchdogHandler.post(permissionWatchdogRunnable)
    }

    private fun stopPermissionWatchdog() {
        permissionWatchdogHandler.removeCallbacks(permissionWatchdogRunnable)
    }

    // ── Countdown ────────────────────────────────────────────────────────────

    private fun startCountdown(durationSeconds: Int) {
        if (isCountdownRunning) {
            Log.w(TAG, "Countdown already running, ignoring duplicate start")
            return
        }

        isCountdownRunning = true
        countdownJob?.cancel()

        countdownJob = lifecycleScope.launch {
            remainingSeconds = durationSeconds
            Log.d(TAG, "Countdown started: $durationSeconds seconds, mode=$currentBlockMode")

            if (currentBlockMode == MODE_SCHEDULED && scheduledEndTimeMs > 0L) {
                // Scheduled/quick block: use absolute end time; counts down even with screen off
                while (isCountdownRunning) {
                    val now = System.currentTimeMillis()
                    val remaining = ((scheduledEndTimeMs - now) / 1000).toInt()
                    if (remaining <= 0) break
                    remainingSeconds = remaining
                    delay(1000)
                }
            } else {
                // Usage-based block: pause when screen is off
                while (remainingSeconds > 0 && isCountdownRunning) {
                    delay(1000)
                    if (isScreenOn) {
                        remainingSeconds--
                    } else {
                        Log.d(TAG, "Countdown paused (screen off), remaining=$remainingSeconds")
                    }
                }
            }

            if (isCountdownRunning) {
                Log.d(TAG, "Countdown finished, dismissing overlay")
                finishBlock()
            }
        }
    }

    private fun finishBlock() {
        Log.d(TAG, "Block finished, removing overlay")
        isCountdownRunning = false
        isOverlayActive = false

        val intent = Intent("com.screenrest.app.ACTION_BLOCK_COMPLETE")
        sendBroadcast(intent)

        stopSelf()
    }

    // ── Feature: Extend time ─────────────────────────────────────────────────

    private fun extendTimeByMinutes(minutes: Int) {
        if (hasBeenExtended) return
        val additionalSeconds = minutes * 60
        if (currentBlockMode == MODE_SCHEDULED && scheduledEndTimeMs > 0L) {
            scheduledEndTimeMs += additionalSeconds.toLong() * 1000
        }
        // Always bump remainingSeconds for immediate UI feedback
        remainingSeconds += additionalSeconds
        hasBeenExtended = true
        Log.d(TAG, "Extended block by $minutes min; new remaining=$remainingSeconds, newEndMs=$scheduledEndTimeMs")
    }

    // ── Feature: Next message ─────────────────────────────────────────────────

    private fun loadNextMessage() {
        lifecycleScope.launch {
            try {
                currentDisplayMessage = getRandomDisplayMessageUseCase()
                currentThemeColor = getNextThemeColor()
                Log.d(TAG, "Loaded next message, theme=$currentThemeColor")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading next message", e)
            }
        }
    }

    // ── Feature: Phone / call mode ────────────────────────────────────────────

    private fun handleOpenPhoneButton() {
        enterPhoneMode()
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(dialIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening phone app", e)
            // If the dialer couldn't open, immediately exit phone mode
            exitPhoneMode()
        }
    }

    private fun enterPhoneMode() {
        if (isPhoneModeActive) return
        isPhoneModeActive = true

        Log.d(TAG, "Entering phone mode")

        // Stop blocking the notification shade
        stopCollapsingPanels()

        // Remove the status bar touch guard so the shade can be pulled down
        try {
            statusBarGuardView?.let { guard ->
                windowManager?.removeView(guard)
                statusBarGuardView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing status bar guard", e)
        }

        // Hide the overlay and make it non-interactive
        try {
            overlayView?.visibility = View.GONE
            overlayParams?.let { p ->
                p.flags = p.flags or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager?.updateViewLayout(overlayView, p)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay for phone mode", e)
        }

        // Auto-restore the overlay after 5 minutes if no call was placed
        phoneModeTimeoutHandler.postDelayed(phoneModeTimeoutRunnable, 5 * 60 * 1000L)
    }

    private fun exitPhoneMode() {
        if (!isPhoneModeActive) return
        isPhoneModeActive = false
        isCallActive = false

        phoneModeTimeoutHandler.removeCallbacks(phoneModeTimeoutRunnable)

        Log.d(TAG, "Exiting phone mode")

        // Restore the overlay
        try {
            overlayView?.visibility = View.VISIBLE
            overlayParams?.let { p ->
                p.flags = p.flags and
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv() and
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                windowManager?.updateViewLayout(overlayView, p)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring overlay after phone mode", e)
        }

        // Re-add the status bar guard
        addStatusBarGuard()

        // Resume blocking the notification shade
        startCollapsingPanels()
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service being destroyed")

        isCountdownRunning = false
        countdownJob?.cancel()
        countdownJob = null

        isCallActive = false
        isPhoneModeActive = false
        phoneModeTimeoutHandler.removeCallbacks(phoneModeTimeoutRunnable)

        try { unregisterReceiver(screenReceiver) } catch (e: Exception) {
            Log.e(TAG, "Error unregistering screen receiver", e)
        }
        try { unregisterReceiver(phoneStateReceiver) } catch (e: Exception) {
            Log.w(TAG, "Error unregistering phone state receiver", e)
        }

        try {
            stopCollapsingPanels()
            stopPermissionWatchdog()

            if (statusBarGuardView != null && windowManager != null) {
                windowManager?.removeView(statusBarGuardView)
                statusBarGuardView = null
            }

            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        }

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        isOverlayActive = false
        currentBlockDurationSeconds = -1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Theme helper ──────────────────────────────────────────────────────────

    private suspend fun getNextThemeColor(): ThemeColor {
        val allColors = ThemeColor.values()
        val currentStoredIndex = messageIndexDataStore.themeIndex.first()
        val nextThemeIndex = (currentStoredIndex + 1) % allColors.size
        messageIndexDataStore.incrementThemeIndex(nextThemeIndex)
        return allColors[nextThemeIndex]
    }
}

// ── Overlay UI ────────────────────────────────────────────────────────────────

@Composable
private fun BlockOverlayContent(
    remainingSeconds: Int,
    displayMessage: DisplayMessage?,
    isDarkTheme: Boolean,
    themeColor: ThemeColor = ThemeColor.TEAL,
    isScheduledBlock: Boolean = false,
    showTimerCountdown: Boolean = true,
    hasBeenExtended: Boolean = false,
    onExtendTime: () -> Unit = {},
    onNextMessage: () -> Unit = {},
    onOpenPhone: () -> Unit = {}
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    val palette = getThemeColorPalette(themeColor)
    val gradientTop = if (isDarkTheme) palette.gradientTopDark else palette.gradientTopLight
    val gradientBottom = if (isDarkTheme) palette.gradientBottomDark else palette.gradientBottomLight
    val timerColor = if (isDarkTheme) palette.primaryLight else palette.primaryVariant
    val messageColor = if (isDarkTheme) palette.textOnDark else palette.textPrimary
    val referenceColor = if (isDarkTheme) palette.secondary else palette.primary
    val dividerColor = if (isDarkTheme) palette.dividerDark else palette.dividerLight
    val subtitleColor = if (isDarkTheme) palette.textMuted else palette.textSecondary
    val buttonBorderColor = if (isDarkTheme) palette.primaryLight.copy(alpha = 0.5f) else palette.primaryVariant.copy(alpha = 0.4f)
    val buttonTextColor = if (isDarkTheme) palette.primaryLight else palette.primaryVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(gradientTop, gradientBottom))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp)
        ) {

            // ── Timer (conditionally shown) ───────────────────────────────
            if (showTimerCountdown) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = timerColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = timerColor,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!isScheduledBlock) {
                Text(
                    text = "Take a break",
                    fontSize = 14.sp,
                    color = subtitleColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                color = dividerColor,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(40.dp))

            // ── Message content ───────────────────────────────────────────
            when (displayMessage) {
                is DisplayMessage.QuranAyah -> {
                    Text(
                        text = displayMessage.ayah.englishTranslation,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = messageColor.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center,
                        lineHeight = 27.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "— Surah ${displayMessage.ayah.surahName} (${displayMessage.ayah.surahNumber}:${displayMessage.ayah.ayahNumber})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        color = referenceColor.copy(alpha = 0.85f)
                    )
                }
                is DisplayMessage.IslamicReminder -> {
                    Text(
                        text = displayMessage.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = messageColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
                null -> { /* nothing */ }
            }

            // ── Action buttons ────────────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                color = dividerColor.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phone / calls button
                OutlinedButton(
                    onClick = onOpenPhone,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = buttonTextColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, buttonBorderColor)
                ) {
                    Text(
                        text = "Phone",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Next message button
                OutlinedButton(
                    onClick = onNextMessage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = buttonTextColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, buttonBorderColor)
                ) {
                    Text(
                        text = "Next",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            // Extend button — only for scheduled/quick blocks, only once
            if (isScheduledBlock && !hasBeenExtended) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onExtendTime,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = buttonTextColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, buttonBorderColor)
                ) {
                    Text(
                        text = "+ Extend 30 min",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Tamper warning — only shown on scheduled profile blocks
            if (isScheduledBlock) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Disabling \"Display over other apps\" will lock your device until this block ends.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
        }
    }
}
