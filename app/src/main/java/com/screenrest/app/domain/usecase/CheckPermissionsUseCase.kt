package com.screenrest.app.domain.usecase

import com.screenrest.app.domain.model.EnforcementLevel
import com.screenrest.app.domain.model.PermissionStatus
import com.screenrest.app.service.PermissionChecker
import javax.inject.Inject

class CheckPermissionsUseCase @Inject constructor(
    private val permissionChecker: PermissionChecker
) {
    
    operator fun invoke(): PermissionStatus {
        return permissionChecker.getAllPermissionStatuses()
    }
    
    fun calculateEnforcementLevel(status: PermissionStatus): EnforcementLevel {
        val hasCorePermissions = status.usageStats && status.overlay
        val hasAccessibility = status.accessibility
        val hasDeviceAdmin = status.deviceAdmin
        
        return when {
            hasCorePermissions && hasAccessibility && hasDeviceAdmin -> EnforcementLevel.FULL
            hasCorePermissions && (hasAccessibility || hasDeviceAdmin) -> EnforcementLevel.STANDARD
            hasCorePermissions -> EnforcementLevel.BASIC
            status.usageStats || status.overlay -> EnforcementLevel.BASIC
            else -> EnforcementLevel.NONE
        }
    }
    
    fun hasMinimumPermissions(status: PermissionStatus): Boolean {
        return status.usageStats && status.overlay
    }
    
    fun getMissingCriticalPermissions(status: PermissionStatus): List<String> {
        val missing = mutableListOf<String>()
        if (!status.usageStats) missing.add("Usage Stats")
        if (!status.overlay) missing.add("Display Over Other Apps")
        return missing
    }
    
    fun getMissingOptionalPermissions(status: PermissionStatus): List<String> {
        val missing = mutableListOf<String>()
        if (!status.accessibility) missing.add("Accessibility Service")
        if (!status.notification) missing.add("Notifications")
        return missing
    }
}
