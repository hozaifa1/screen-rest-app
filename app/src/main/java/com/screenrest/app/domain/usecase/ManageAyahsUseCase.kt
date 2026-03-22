package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.AyahDatabaseRepository
import com.screenrest.app.domain.model.Ayah
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageAyahsUseCase @Inject constructor(
    private val ayahDatabaseRepository: AyahDatabaseRepository
) {

    fun getAllAyahs(): Flow<List<Ayah>> {
        return ayahDatabaseRepository.getAllAyahs()
    }

    suspend fun addAyah(ayah: Ayah): Result<Long> {
        return try {
            if (ayah.englishTranslation.isBlank()) {
                return Result.failure(IllegalArgumentException("Ayah translation cannot be empty"))
            }
            val id = ayahDatabaseRepository.insertAyah(ayah)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAyah(ayah: Ayah): Result<Unit> {
        return try {
            if (ayah.englishTranslation.isBlank()) {
                return Result.failure(IllegalArgumentException("Ayah translation cannot be empty"))
            }
            ayahDatabaseRepository.updateAyah(ayah)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAyah(id: Long): Result<Unit> {
        return try {
            ayahDatabaseRepository.deleteAyahById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureDefaults() {
        ayahDatabaseRepository.ensureDefaults()
    }
}
