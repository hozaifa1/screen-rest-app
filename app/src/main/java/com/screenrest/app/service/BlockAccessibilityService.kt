package com.screenrest.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.screenrest.app.presentation.block.BlockActivity

class BlockAccessibilityService : AccessibilityService() {
    
    companion object {
        @Volatile
        var isBlockActive = false
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isBlockActive) return
        
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = it.packageName?.toString() ?: return
                
                if (packageName != "com.screenrest.app") {
                    relaunchBlockActivity()
                }
            }
        }
    }
    
    override fun onInterrupt() {
        // Handle interruption if needed
    }
    
    private fun relaunchBlockActivity() {
        val intent = Intent(this, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
}
