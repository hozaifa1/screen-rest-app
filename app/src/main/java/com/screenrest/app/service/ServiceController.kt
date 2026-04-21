package com.screenrest.app.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object ServiceController {
    
    fun startTracking(context: Context) {
        val intent = Intent(context, UsageTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopTracking(context: Context) {
        val intent = Intent(context, UsageTrackingService::class.java)
        context.stopService(intent)
    }
    
    /**
     * Start the BlockTimeSchedulerService. Must be called:
     * - On app launch (if onboarding complete)
     * - After saving/deleting/toggling a block time profile
     * - On boot (via BootReceiver)
     */
    fun startBlockTimeScheduler(context: Context) {
        val intent = Intent(context, BlockTimeSchedulerService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }
    
    fun isRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (UsageTrackingService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
