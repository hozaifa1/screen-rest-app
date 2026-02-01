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
        return when {
            status.usageStats && status.overlay && status.accessibility -> {
                EnforcementLevel.FULL
            }
            status.usageStats && status.overlay -> {
                EnforcementLevel.STANDARD
            }
            status.usageStats || status.overlay -> {
                EnforcementLevel.BASIC
            }
            else -> {
                EnforcementLevel.NONE
            }
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
