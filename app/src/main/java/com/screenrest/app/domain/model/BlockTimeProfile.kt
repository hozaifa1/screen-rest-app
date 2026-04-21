package com.screenrest.app.domain.model

data class BlockTimeProfile(
    val id: Long = 0,
    val name: String = "",
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeek: Set<Int> = emptySet(),   // 1=Mon..7=Sun
    val isEnabled: Boolean = false,
    val customMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val passwordHash: String? = null,
    val timeLockUntilMillis: Long = 0
) {
    val isPasswordLocked: Boolean get() = passwordHash != null
    val isTimeLocked: Boolean get() = timeLockUntilMillis > System.currentTimeMillis()
    val isLocked: Boolean get() = isPasswordLocked || isTimeLocked

    /** Duration in minutes. Handles cross-midnight. */
    val durationMinutes: Int get() {
        return if (endMinuteOfDay >= startMinuteOfDay) {
            endMinuteOfDay - startMinuteOfDay
        } else {
            (1440 - startMinuteOfDay) + endMinuteOfDay
        }
    }
}
