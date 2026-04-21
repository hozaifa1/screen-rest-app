package com.screenrest.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class BlockAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "BlockAccessibility"
        
        @Volatile
        var isBlockActive = false
    }

    // Blocked settings screens — checked during active blocks only
    private val blockedActivities = setOf(
        // Standard Android Settings
        "com.android.settings.applications.InstalledAppDetailsTop",
        "com.android.settings.applications.InstalledAppDetails",
        "com.android.settings.applications.AppInfoDashboard",
        "com.android.settings.applications.AppInfoBase",
        "com.android.settings.accessibility.AccessibilitySettings",
        "com.android.settings.accessibility.AccessibilityDetailsSettingsFragment",
        "com.android.settings.accessibility.ToggleAccessibilityServicePreferenceFragment",
        "com.android.settings.DeviceAdminSettings",
        "com.android.settings.DeviceAdminAdd",
        "com.android.settings.applications.manageapplications.ManageApplications",
        // Overlay permissions (stock Android + Samsung + OEM)
        "com.android.settings.applications.appinfo.DrawOverlayDetails",
        "com.android.settings.applications.appinfo.ManageExternalStorageDetails",
        "com.android.settings.Settings\$AppDrawOverlaySettingsActivity",
        "com.android.settings.Settings\$OverlaySettingsActivity",
        "com.android.settings.Settings\$ManageAppOverlayPermissionActivity",
        // Samsung wrapper activities (used for all sub-settings)
        "com.android.settings.SubSettings",
        "com.samsung.android.settings.SubSettings",
    )

    // Also check by partial class name match (more resilient across Android versions)
    private val blockedClassSubstrings = setOf(
        "InstalledAppDetails", "AppInfoDashboard", "AppInfo",
        "AccessibilitySettings", "AccessibilityDetails",
        "DeviceAdmin", "DrawOverlay", "ManageOverlay",
        "AppearOnTop", "OverlaySettings", "DisplayOverOther",
        "FloatingPermission", "OverlayPermission",
        "SubSettings",   // Samsung/stock wrapper for deep settings screens
        "ForceStop", "Uninstall", "ClearData",
    )

    // Package-level blocks (OEM security centers + package installer)
    private val blockedPackages = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.securitycenter",
        "com.coloros.safecenter",
        "com.samsung.android.sm",
    )

    private val handler = Handler(Looper.getMainLooper())
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val isBlockCurrentlyActive = BlockOverlayService.isOverlayActive || isBlockActive
            
            if (!isBlockCurrentlyActive) return

            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: ""

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // 1. Collapse notification shade / block system UI
                    if (packageName == "com.android.systemui") {
                        // Allow notification shade during active/incoming calls so the user
                        // can answer or interact with the call notification.
                        if (BlockOverlayService.isCallActive) {
                            Log.d(TAG, "System UI during active call — allowing")
                            return
                        }
                        Log.d(TAG, "System UI detected during block - closing")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        if (Build.VERSION.SDK_INT >= 31) {
                            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                        }
                        // Detect recents
                        if (className.contains("recents", ignoreCase = true)) {
                            handler.postDelayed({
                                performGlobalAction(GLOBAL_ACTION_HOME)
                            }, 50)
                        }
                        return
                    }

                    // 2. Block settings navigation (anti-bypass)
                    if (shouldBlockSettingsScreen(packageName, className)) {
                        Log.d(TAG, "Blocked settings navigation: $packageName/$className")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        handler.postDelayed({
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }, 50)
                        return
                    }

                    // 3. Block uninstall flows / OEM security centers
                    if (packageName in blockedPackages) {
                        Log.d(TAG, "Blocked package: $packageName")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        return
                    }

                    // 4. Block power menu
                    if (className.contains("globalactions", ignoreCase = true) ||
                        className.contains("shutdownthread", ignoreCase = true)) {
                        Log.d(TAG, "Blocked power menu: $className")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent", e)
        }
    }

    private fun shouldBlockSettingsScreen(packageName: String, className: String): Boolean {
        // Block ALL app info / accessibility / admin settings during a block
        // (not just our app's — user shouldn't be in Settings at all during a block)
        val isSettingsPackage = packageName == "com.android.settings" ||
                packageName == "com.samsung.android.settings" ||
                packageName == "com.coloros.settings" ||         // OPPO/Realme
                packageName == "com.oplus.settings" ||           // OnePlus
                packageName == "com.miui.settings" ||            // Xiaomi
                packageName == "com.huawei.systemmanager"        // Huawei
                
        if (isSettingsPackage) {
            if (className in blockedActivities) return true
            return blockedClassSubstrings.any { className.contains(it, ignoreCase = true) }
        }
        return false
    }
    
    override fun onInterrupt() {}
}
