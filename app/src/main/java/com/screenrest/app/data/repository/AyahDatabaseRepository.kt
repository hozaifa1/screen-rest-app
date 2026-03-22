package com.screenrest.app.data.repository

import com.screenrest.app.data.local.LocalAyahProvider
import com.screenrest.app.data.local.database.dao.AyahDao
import com.screenrest.app.data.local.database.entity.toDomain
import com.screenrest.app.data.local.database.entity.toEntity
import com.screenrest.app.domain.model.Ayah
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AyahDatabaseRepository {
    fun getAllAyahs(): Flow<List<Ayah>>
    suspend fun getAyahById(id: Long): Ayah?
    suspend fun insertAyah(ayah: Ayah): Long
    suspend fun updateAyah(ayah: Ayah)
    suspend fun deleteAyah(ayah: Ayah)
    suspend fun deleteAyahById(id: Long)
    suspend fun deleteAllAyahs()
    suspend fun getAyahCount(): Int
    suspend fun ensureDefaults()
}

@Singleton
class AyahDatabaseRepositoryImpl @Inject constructor(
    private val ayahDao: AyahDao,
    private val localAyahProvider: LocalAyahProvider
) : AyahDatabaseRepository {

    override fun getAllAyahs(): Flow<List<Ayah>> {
        return ayahDao.getAllAyahs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAyahById(id: Long): Ayah? {
        return ayahDao.getAyahById(id)?.toDomain()
    }

    override suspend fun insertAyah(ayah: Ayah): Long {
        return ayahDao.insertAyah(ayah.toEntity())
    }

    override suspend fun updateAyah(ayah: Ayah) {
        ayahDao.updateAyah(ayah.toEntity())
    }

    override suspend fun deleteAyah(ayah: Ayah) {
        ayahDao.deleteAyah(ayah.toEntity())
    }

    override suspend fun deleteAyahById(id: Long) {
        ayahDao.deleteAyahById(id)
    }

    override suspend fun deleteAllAyahs() {
        ayahDao.deleteAllAyahs()
    }

    override suspend fun getAyahCount(): Int {
        return ayahDao.getAyahCount()
    }

    override suspend fun ensureDefaults() {
        val count = ayahDao.getAyahCount()
        if (count == 0) {
            val defaultAyahs = localAyahProvider.getAllLocalAyahs()
            val now = System.currentTimeMillis()
            val ayahEntities = defaultAyahs.mapIndexed { index, ayah ->
                ayah.toEntity(createdAt = now - index)
            }
            ayahDao.insertAll(ayahEntities)
        }
    }
}
