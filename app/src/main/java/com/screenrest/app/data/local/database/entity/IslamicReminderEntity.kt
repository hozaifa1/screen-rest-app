package com.screenrest.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.screenrest.app.domain.model.IslamicReminder

@Entity(tableName = "islamic_reminders")
data class IslamicReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val createdAt: Long
)

fun IslamicReminderEntity.toDomain(): IslamicReminder {
    return IslamicReminder(
        id = id,
        text = text,
        createdAt = createdAt
    )
}

fun IslamicReminder.toEntity(): IslamicReminderEntity {
    return IslamicReminderEntity(
        id = id,
        text = text,
        createdAt = createdAt
    )
}
