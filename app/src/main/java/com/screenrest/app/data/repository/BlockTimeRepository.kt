package com.screenrest.app.data.repository

import com.screenrest.app.domain.model.BlockTimeProfile
import kotlinx.coroutines.flow.Flow

interface BlockTimeRepository {
    fun getAllProfiles(): Flow<List<BlockTimeProfile>>
    fun getEnabledProfiles(): Flow<List<BlockTimeProfile>>
    suspend fun getProfileById(id: Long): BlockTimeProfile?
    suspend fun saveProfile(profile: BlockTimeProfile): Long
    suspend fun deleteProfile(profile: BlockTimeProfile)
    suspend fun getEnabledProfilesOnce(): List<BlockTimeProfile>
}
