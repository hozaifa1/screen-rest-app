package com.screenrest.app.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.screenrest.app.receiver.ScreenRestDeviceAdminReceiver

object DeviceAdminHelper {
    private fun getComponentName(context: Context): ComponentName {
        return ComponentName(context, ScreenRestDeviceAdminReceiver::class.java)
    }

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(getComponentName(context))
    }

    fun requestActivation(activity: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(activity))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "ScreenRest needs device admin to prevent app uninstallation during scheduled phone blocks. " +
                "This ensures your break schedule cannot be bypassed."
            )
        }
        activity.startActivity(intent)
    }

    fun forceLockScreen(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(getComponentName(context))) {
            dpm.lockNow()
        }
    }
}
