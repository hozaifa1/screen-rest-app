package com.screenrest.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class ScreenRestDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // Device admin activated — log or show toast
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // This message is shown when user tries to deactivate device admin
        // It's the LAST line of defense before they can uninstall
        return "Deactivating device admin will remove phone block protection. " +
               "Active blocks will no longer be enforced."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Device admin was deactivated
        // We can't prevent this, but we can:
        // 1. Show a persistent high-priority notification urging re-activation
        // 2. If a block is currently active, re-show the overlay (it still works without admin)
    }
}
