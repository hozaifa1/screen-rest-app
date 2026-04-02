package com.screenrest.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.screenrest.app.data.local.database.dao.AyahDao
import com.screenrest.app.data.local.database.dao.CustomMessageDao
import com.screenrest.app.data.local.database.dao.IslamicReminderDao
import com.screenrest.app.data.local.database.entity.AyahEntity
import com.screenrest.app.data.local.database.entity.CustomMessageEntity
import com.screenrest.app.data.local.database.entity.IslamicReminderEntity

@Database(
    entities = [CustomMessageEntity::class, IslamicReminderEntity::class, AyahEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customMessageDao(): CustomMessageDao
    abstract fun islamicReminderDao(): IslamicReminderDao
    abstract fun ayahDao(): AyahDao
}
