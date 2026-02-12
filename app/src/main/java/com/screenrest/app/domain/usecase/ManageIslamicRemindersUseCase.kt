package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.IslamicReminderRepository
import com.screenrest.app.domain.model.IslamicReminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageIslamicRemindersUseCase @Inject constructor(
    private val islamicReminderRepository: IslamicReminderRepository
) {

    fun getAllReminders(): Flow<List<IslamicReminder>> {
        return islamicReminderRepository.getAllReminders()
    }

    suspend fun addReminder(text: String): Result<Long> {
        return try {
            if (text.isBlank()) {
                return Result.failure(IllegalArgumentException("Reminder text cannot be empty"))
            }
            if (text.length > 500) {
                return Result.failure(IllegalArgumentException("Reminder text too long (max 500 characters)"))
            }

            val reminder = IslamicReminder(
                id = 0,
                text = text.trim(),
                createdAt = System.currentTimeMillis()
            )
            val id = islamicReminderRepository.insertReminder(reminder)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(id: Long): Result<Unit> {
        return try {
            islamicReminderRepository.deleteReminderById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureDefaults() {
        islamicReminderRepository.ensureDefaults()
    }
}
