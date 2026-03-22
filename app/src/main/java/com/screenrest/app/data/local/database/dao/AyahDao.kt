package com.screenrest.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.screenrest.app.data.local.database.entity.AyahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AyahDao {

    @Query("SELECT * FROM ayahs ORDER BY createdAt DESC")
    fun getAllAyahs(): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE id = :id")
    suspend fun getAyahById(id: Long): AyahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyah(ayah: AyahEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ayahs: List<AyahEntity>)

    @Update
    suspend fun updateAyah(ayah: AyahEntity)

    @Delete
    suspend fun deleteAyah(ayah: AyahEntity)

    @Query("DELETE FROM ayahs WHERE id = :id")
    suspend fun deleteAyahById(id: Long)

    @Query("DELETE FROM ayahs")
    suspend fun deleteAllAyahs()

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun getAyahCount(): Int
}
