package com.screenrest.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.screenrest.app.R
import com.screenrest.app.data.repository.BlockTimeRepository
import com.screenrest.app.domain.model.BlockTimeProfile
import com.screenrest.app.receiver.BlockTimeAlarmReceiver
import com.screenrest.app.util.DeviceAdminHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class BlockTimeSchedulerService : LifecycleService() {

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "block_time_scheduler"
        private const val CHECK_INTERVAL_MS = 30_000L
    }

    @Inject
    lateinit var blockTimeRepository: BlockTimeRepository

    @Inject
    lateinit var settingsRepository: com.screenrest.app.data.repository.SettingsRepository

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch {
                checkAndEnforce()
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, createNotification())
        // Remove any existing callbacks to avoid duplicate runnables on repeated onStartCommand
        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val schedulerChannel = NotificationChannel(
                CHANNEL_ID,
                "Block Time Scheduler",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors scheduled phone block times"
                setShowBadge(false)
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(schedulerChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ScreenRest")
            .setContentText("Monitoring scheduled blocks")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private suspend fun checkAndEnforce() {
        val profiles = blockTimeRepository.getEnabledProfilesOnce()
        val now = LocalTime.now()
        val today = LocalDate.now().dayOfWeek.value // 1=Mon..7=Sun
        val nowMinutes = now.hour * 60 + now.minute

        val activeProfile = profiles.firstOrNull { isWithinBlock(it, nowMinutes, today) }

        if (activeProfile != null && !BlockOverlayService.isOverlayActive) {
            if (Settings.canDrawOverlays(this)) {
                // Overlay permission available — show the block overlay
                launchBlockOverlay(activeProfile, nowMinutes)
            } else {
                // Overlay permission REVOKED during active block — lock device as fallback
                android.util.Log.w("BlockTimeScheduler",
                    "Active block '${activeProfile.name}' but overlay permission revoked — locking device")
                DeviceAdminHelper.forceLockScreen(this)
            }
        } else if (activeProfile == null && BlockOverlayService.isOverlayActive) {
            // No active scheduled block but overlay is showing — 
            // only dismiss if overlay was started by scheduler (check block mode)
            // The overlay will auto-dismiss via its own countdown, but this is a safety net
            // for cases like profile deletion/disable during active block
        }

        // Auto-lock enforcement: lock profiles 30 min before their start time
        enforceAutoLock(profiles, nowMinutes, today)

        scheduleNextAlarm(profiles, nowMinutes, today)
    }

    private suspend fun enforceAutoLock(profiles: List<BlockTimeProfile>, nowMinutes: Int, today: Int) {
        try {
            val autoLockEnabled = settingsRepository.getAutoLockBeforeBlock().first()
            if (!autoLockEnabled) return

            // For each enabled profile, check if its start time is within 30 minutes
            for (profile in profiles) {
                if (profile.isLocked) continue // Already locked

                val startsToday = today in profile.daysOfWeek
                if (startsToday) {
                    val minutesUntilStart = profile.startMinuteOfDay - nowMinutes
                    if (minutesUntilStart in 1..30) {
                        // Within 30 min window — auto-lock with a 30min time lock
                        val lockUntilMs = System.currentTimeMillis() + (minutesUntilStart * 60 * 1000L) + (profile.durationMinutes * 60 * 1000L)
                        val lockedProfile = profile.copy(
                            timeLockUntilMillis = lockUntilMs
                        )
                        blockTimeRepository.saveProfile(lockedProfile)
                        android.util.Log.d("BlockTimeScheduler", "Auto-locked profile '${profile.name}' (${minutesUntilStart}min before block)")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BlockTimeScheduler", "Error in auto-lock enforcement", e)
        }
    }

    private fun isWithinBlock(profile: BlockTimeProfile, nowMinutes: Int, dayOfWeek: Int): Boolean {
        if (dayOfWeek !in profile.daysOfWeek) {
            // Check if yesterday's cross-midnight block is still active
            val yesterday = if (dayOfWeek == 1) 7 else dayOfWeek - 1
            if (yesterday !in profile.daysOfWeek) return false
            // Yesterday's block that crosses midnight
            if (profile.endMinuteOfDay <= profile.startMinuteOfDay) {
                return nowMinutes < profile.endMinuteOfDay
            }
            return false
        }

        return if (profile.endMinuteOfDay >= profile.startMinuteOfDay) {
            nowMinutes in profile.startMinuteOfDay until profile.endMinuteOfDay
        } else {
            // Cross-midnight: active from start..midnight OR midnight..end
            nowMinutes >= profile.startMinuteOfDay || nowMinutes < profile.endMinuteOfDay
        }
    }

    private fun launchBlockOverlay(profile: BlockTimeProfile, nowMinutes: Int) {
        // Calculate remaining seconds until end
        val remainingMinutes = if (profile.endMinuteOfDay >= nowMinutes) {
            profile.endMinuteOfDay - nowMinutes
        } else {
            // Cross-midnight
            (1440 - nowMinutes) + profile.endMinuteOfDay
        }
        val remainingSeconds = remainingMinutes * 60
        
        // Calculate absolute end time in epoch ms for accurate countdown
        val endTimeMs = System.currentTimeMillis() + (remainingSeconds * 1000L)

        val intent = Intent(this, BlockOverlayService::class.java).apply {
            putExtra(BlockOverlayService.EXTRA_DURATION_SECONDS, remainingSeconds)
            putExtra(BlockOverlayService.EXTRA_BLOCK_MODE, BlockOverlayService.MODE_SCHEDULED)
            putExtra(BlockOverlayService.EXTRA_END_TIME_MS, endTimeMs)
            profile.customMessage?.let {
                putExtra(BlockOverlayService.EXTRA_CUSTOM_MESSAGE, it)
            }
        }
        startService(intent)
    }

    private fun scheduleNextAlarm(profiles: List<BlockTimeProfile>, nowMinutes: Int, today: Int) {
        if (profiles.isEmpty()) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Find the soonest event: next block start OR current block end
        var bestMinutesFromNow: Int? = null

        for (profile in profiles) {
            // 1. Check if currently within this block — schedule alarm at end time
            if (isWithinBlock(profile, nowMinutes, today)) {
                val minutesToEnd = if (profile.endMinuteOfDay > nowMinutes) {
                    profile.endMinuteOfDay - nowMinutes
                } else {
                    // Cross-midnight: end is tomorrow
                    (1440 - nowMinutes) + profile.endMinuteOfDay
                }
                if (bestMinutesFromNow == null || minutesToEnd < bestMinutesFromNow) {
                    bestMinutesFromNow = minutesToEnd
                }
            }

            // 2. Check for upcoming starts today
            if (today in profile.daysOfWeek && profile.startMinuteOfDay > nowMinutes) {
                val minutesToStart = profile.startMinuteOfDay - nowMinutes
                if (bestMinutesFromNow == null || minutesToStart < bestMinutesFromNow) {
                    bestMinutesFromNow = minutesToStart
                }
            }

            // 3. Check for upcoming starts on future days (up to 7 days ahead)
            if (bestMinutesFromNow == null) {
                for (d in 1..7) {
                    val checkDay = ((today - 1 + d) % 7) + 1
                    if (checkDay in profile.daysOfWeek) {
                        val minutesToStart = (d * 1440) + profile.startMinuteOfDay - nowMinutes
                        if (bestMinutesFromNow == null || minutesToStart < bestMinutesFromNow) {
                            bestMinutesFromNow = minutesToStart
                        }
                        break
                    }
                }
            }
        }

        if (bestMinutesFromNow != null && bestMinutesFromNow > 0) {
            val triggerTimeMs = System.currentTimeMillis() + (bestMinutesFromNow * 60 * 1000L)

            val intent = Intent(this, BlockTimeAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use setAlarmClock for reliability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent),
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent),
                    pendingIntent
                )
            }
        }
    }


}
