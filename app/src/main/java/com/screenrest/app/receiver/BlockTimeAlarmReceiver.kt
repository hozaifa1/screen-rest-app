package com.screenrest.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.screenrest.app.service.BlockTimeSchedulerService

class BlockTimeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Simply restart the scheduler service — it will check and enforce
        val serviceIntent = Intent(context, BlockTimeSchedulerService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
