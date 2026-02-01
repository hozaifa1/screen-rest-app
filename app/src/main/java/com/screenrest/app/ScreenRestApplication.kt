package com.screenrest.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScreenRestApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            val trackingChannel = NotificationChannel(
                "screenrest_tracking",
                "Screen Time Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your screen time usage"
                setShowBadge(false)
            }
            
            val blockChannel = NotificationChannel(
                "screenrest_block",
                "Break Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for screen breaks"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(blockChannel)
        }
    }
}
