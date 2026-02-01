package com.screenrest.app.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.screenrest.app.domain.model.CustomMessage

@Entity(tableName = "custom_messages")
data class CustomMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val createdAt: Long
)

fun CustomMessageEntity.toDomain(): CustomMessage {
    return CustomMessage(
        id = id,
        text = text,
        createdAt = createdAt
    )
}

fun CustomMessage.toEntity(): CustomMessageEntity {
    return CustomMessageEntity(
        id = id,
        text = text,
        createdAt = createdAt
    )
}
