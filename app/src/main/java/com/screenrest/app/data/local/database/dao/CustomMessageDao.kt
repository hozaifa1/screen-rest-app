package com.screenrest.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screenrest.app.data.local.database.entity.CustomMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomMessageDao {
    
    @Query("SELECT * FROM custom_messages ORDER BY createdAt DESC")
    fun getAllMessages(): Flow<List<CustomMessageEntity>>
    
    @Query("SELECT * FROM custom_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): CustomMessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CustomMessageEntity): Long
    
    @Update
    suspend fun updateMessage(message: CustomMessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: CustomMessageEntity)
    
    @Query("DELETE FROM custom_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
    
    @Query("DELETE FROM custom_messages")
    suspend fun deleteAllMessages()
    
    @Query("SELECT COUNT(*) FROM custom_messages")
    suspend fun getMessageCount(): Int
}
