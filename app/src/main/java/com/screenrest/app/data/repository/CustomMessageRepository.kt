package com.screenrest.app.data.repository

import com.screenrest.app.data.local.database.dao.CustomMessageDao
import com.screenrest.app.data.local.database.entity.toDomain
import com.screenrest.app.data.local.database.entity.toEntity
import com.screenrest.app.domain.model.CustomMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface CustomMessageRepository {
    fun getAllMessages(): Flow<List<CustomMessage>>
    suspend fun getMessageById(id: Long): CustomMessage?
    suspend fun insertMessage(message: CustomMessage): Long
    suspend fun updateMessage(message: CustomMessage)
    suspend fun deleteMessage(message: CustomMessage)
    suspend fun deleteMessageById(id: Long)
    suspend fun deleteAllMessages()
    suspend fun getMessageCount(): Int
}

@Singleton
class CustomMessageRepositoryImpl @Inject constructor(
    private val customMessageDao: CustomMessageDao
) : CustomMessageRepository {
    
    override fun getAllMessages(): Flow<List<CustomMessage>> {
        return customMessageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getMessageById(id: Long): CustomMessage? {
        return customMessageDao.getMessageById(id)?.toDomain()
    }
    
    override suspend fun insertMessage(message: CustomMessage): Long {
        return customMessageDao.insertMessage(message.toEntity())
    }
    
    override suspend fun updateMessage(message: CustomMessage) {
        customMessageDao.updateMessage(message.toEntity())
    }
    
    override suspend fun deleteMessage(message: CustomMessage) {
        customMessageDao.deleteMessage(message.toEntity())
    }
    
    override suspend fun deleteMessageById(id: Long) {
        customMessageDao.deleteMessageById(id)
    }
    
    override suspend fun deleteAllMessages() {
        customMessageDao.deleteAllMessages()
    }
    
    override suspend fun getMessageCount(): Int {
        return customMessageDao.getMessageCount()
    }
}
