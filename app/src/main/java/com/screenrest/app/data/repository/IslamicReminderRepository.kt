package com.screenrest.app.data.repository

import com.screenrest.app.data.local.database.dao.IslamicReminderDao
import com.screenrest.app.data.local.database.entity.IslamicReminderEntity
import com.screenrest.app.data.local.database.entity.toDomain
import com.screenrest.app.data.local.database.entity.toEntity
import com.screenrest.app.domain.model.DefaultIslamicReminders
import com.screenrest.app.domain.model.IslamicReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface IslamicReminderRepository {
    fun getAllReminders(): Flow<List<IslamicReminder>>
    suspend fun getReminderById(id: Long): IslamicReminder?
    suspend fun insertReminder(reminder: IslamicReminder): Long
    suspend fun updateReminder(reminder: IslamicReminder)
    suspend fun deleteReminder(reminder: IslamicReminder)
    suspend fun deleteReminderById(id: Long)
    suspend fun deleteAllReminders()
    suspend fun getReminderCount(): Int
    suspend fun ensureDefaults()
}

@Singleton
class IslamicReminderRepositoryImpl @Inject constructor(
    private val islamicReminderDao: IslamicReminderDao
) : IslamicReminderRepository {

    override fun getAllReminders(): Flow<List<IslamicReminder>> {
        return islamicReminderDao.getAllReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getReminderById(id: Long): IslamicReminder? {
        return islamicReminderDao.getReminderById(id)?.toDomain()
    }

    override suspend fun insertReminder(reminder: IslamicReminder): Long {
        return islamicReminderDao.insertReminder(reminder.toEntity())
    }

    override suspend fun updateReminder(reminder: IslamicReminder) {
        islamicReminderDao.updateReminder(reminder.toEntity())
    }

    override suspend fun deleteReminder(reminder: IslamicReminder) {
        islamicReminderDao.deleteReminder(reminder.toEntity())
    }

    override suspend fun deleteReminderById(id: Long) {
        islamicReminderDao.deleteReminderById(id)
    }

    override suspend fun deleteAllReminders() {
        islamicReminderDao.deleteAllReminders()
    }

    override suspend fun getReminderCount(): Int {
        return islamicReminderDao.getReminderCount()
    }

    override suspend fun ensureDefaults() {
        val count = islamicReminderDao.getReminderCount()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val defaults = DefaultIslamicReminders.list.mapIndexed { index, text ->
                IslamicReminderEntity(
                    text = text,
                    createdAt = now - index
                )
            }
            islamicReminderDao.insertAll(defaults)
        }
    }
}
