package com.screenrest.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.screenrest.app.data.local.database.dao.CustomMessageDao
import com.screenrest.app.data.local.database.entity.CustomMessageEntity

@Database(
    entities = [CustomMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customMessageDao(): CustomMessageDao
}
