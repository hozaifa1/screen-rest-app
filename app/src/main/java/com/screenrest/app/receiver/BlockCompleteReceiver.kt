package com.screenrest.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenrest.app.service.BlockAccessibilityService

class BlockCompleteReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.screenrest.app.ACTION_BLOCK_COMPLETE") {
            BlockAccessibilityService.isBlockActive = false
        }
    }
}
