package com.screenrest.app.data.repository

import com.screenrest.app.data.local.database.dao.BlockTimeProfileDao
import com.screenrest.app.data.local.database.entity.BlockTimeProfileEntity
import com.screenrest.app.domain.model.BlockTimeProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BlockTimeRepositoryImpl @Inject constructor(
    private val dao: BlockTimeProfileDao
) : BlockTimeRepository {

    override fun getAllProfiles(): Flow<List<BlockTimeProfile>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getEnabledProfiles(): Flow<List<BlockTimeProfile>> {
        return dao.getEnabled().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getProfileById(id: Long): BlockTimeProfile? {
        return dao.getById(id)?.toDomainModel()
    }

    override suspend fun saveProfile(profile: BlockTimeProfile): Long {
        return dao.insert(profile.toEntity())
    }

    override suspend fun deleteProfile(profile: BlockTimeProfile) {
        dao.delete(profile.toEntity())
    }

    override suspend fun getEnabledProfilesOnce(): List<BlockTimeProfile> {
        return dao.getEnabledOnce().map { it.toDomainModel() }
    }

    private fun BlockTimeProfileEntity.toDomainModel(): BlockTimeProfile {
        val days = if (daysOfWeek.isBlank()) {
            emptySet()
        } else {
            daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
        return BlockTimeProfile(
            id = id,
            name = name,
            startMinuteOfDay = startMinuteOfDay,
            endMinuteOfDay = endMinuteOfDay,
            daysOfWeek = days,
            isEnabled = isEnabled,
            customMessage = customMessage,
            createdAt = createdAt,
            passwordHash = passwordHash,
            timeLockUntilMillis = timeLockUntilMillis
        )
    }

    private fun BlockTimeProfile.toEntity(): BlockTimeProfileEntity {
        return BlockTimeProfileEntity(
            id = id,
            name = name,
            startMinuteOfDay = startMinuteOfDay,
            endMinuteOfDay = endMinuteOfDay,
            daysOfWeek = daysOfWeek.joinToString(","),
            isEnabled = isEnabled,
            customMessage = customMessage,
            createdAt = createdAt,
            passwordHash = passwordHash,
            timeLockUntilMillis = timeLockUntilMillis
        )
    }
}
