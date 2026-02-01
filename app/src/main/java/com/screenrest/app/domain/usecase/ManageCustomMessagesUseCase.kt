package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.CustomMessageRepository
import com.screenrest.app.domain.model.CustomMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageCustomMessagesUseCase @Inject constructor(
    private val customMessageRepository: CustomMessageRepository
) {
    
    fun getAllMessages(): Flow<List<CustomMessage>> {
        return customMessageRepository.getAllMessages()
    }
    
    suspend fun addMessage(text: String): Result<Long> {
        return try {
            if (text.isBlank()) {
                return Result.failure(IllegalArgumentException("Message text cannot be empty"))
            }
            if (text.length > 500) {
                return Result.failure(IllegalArgumentException("Message text too long (max 500 characters)"))
            }
            
            val message = CustomMessage(
                id = 0,
                text = text.trim(),
                createdAt = System.currentTimeMillis()
            )
            val id = customMessageRepository.insertMessage(message)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMessage(id: Long, text: String): Result<Unit> {
        return try {
            if (text.isBlank()) {
                return Result.failure(IllegalArgumentException("Message text cannot be empty"))
            }
            if (text.length > 500) {
                return Result.failure(IllegalArgumentException("Message text too long (max 500 characters)"))
            }
            
            val existingMessage = customMessageRepository.getMessageById(id)
                ?: return Result.failure(IllegalArgumentException("Message not found"))
            
            val updatedMessage = existingMessage.copy(text = text.trim())
            customMessageRepository.updateMessage(updatedMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMessage(id: Long): Result<Unit> {
        return try {
            customMessageRepository.deleteMessageById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAllMessages(): Result<Unit> {
        return try {
            customMessageRepository.deleteAllMessages()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
