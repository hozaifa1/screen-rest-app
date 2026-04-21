package com.screenrest.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_time_profiles")
data class BlockTimeProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val startMinuteOfDay: Int,        // 0..1439 (minutes from midnight)
    val endMinuteOfDay: Int,          // 0..1439, if < start → cross-midnight
    val daysOfWeek: String,           // comma-separated "1,2,3" Mon=1..Sun=7
    val isEnabled: Boolean = false,
    val customMessage: String? = null, // user-defined break message, null = use random ayah/reminder
    val createdAt: Long = System.currentTimeMillis(),

    // Per-profile lock fields
    val passwordHash: String? = null,  // SHA-256, null = no password lock
    val timeLockUntilMillis: Long = 0  // 0 = no time lock, >0 = locked until epoch ms
)
