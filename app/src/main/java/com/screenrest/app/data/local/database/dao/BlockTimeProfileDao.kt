package com.screenrest.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screenrest.app.data.local.database.entity.BlockTimeProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockTimeProfileDao {
    @Query("SELECT * FROM block_time_profiles ORDER BY startMinuteOfDay ASC")
    fun getAll(): Flow<List<BlockTimeProfileEntity>>

    @Query("SELECT * FROM block_time_profiles WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<BlockTimeProfileEntity>>

    @Query("SELECT * FROM block_time_profiles WHERE id = :id")
    suspend fun getById(id: Long): BlockTimeProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: BlockTimeProfileEntity): Long

    @Update
    suspend fun update(profile: BlockTimeProfileEntity)

    @Delete
    suspend fun delete(profile: BlockTimeProfileEntity)

    @Query("SELECT * FROM block_time_profiles WHERE isEnabled = 1")
    suspend fun getEnabledOnce(): List<BlockTimeProfileEntity>
}
