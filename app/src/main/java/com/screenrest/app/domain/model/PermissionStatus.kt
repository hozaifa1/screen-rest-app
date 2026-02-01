package com.screenrest.app.domain.model

data class PermissionStatus(
    val usageStats: Boolean = false,
    val overlay: Boolean = false,
    val accessibility: Boolean = false,
    val notification: Boolean = false,
    val location: Boolean = false,
    val backgroundLocation: Boolean = false
)
