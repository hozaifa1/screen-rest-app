package com.screenrest.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screenrest.app.data.local.database.entity.IslamicReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IslamicReminderDao {

    @Query("SELECT * FROM islamic_reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<IslamicReminderEntity>>

    @Query("SELECT * FROM islamic_reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): IslamicReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: IslamicReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<IslamicReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: IslamicReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: IslamicReminderEntity)

    @Query("DELETE FROM islamic_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM islamic_reminders")
    suspend fun deleteAllReminders()

    @Query("SELECT COUNT(*) FROM islamic_reminders")
    suspend fun getReminderCount(): Int
}
