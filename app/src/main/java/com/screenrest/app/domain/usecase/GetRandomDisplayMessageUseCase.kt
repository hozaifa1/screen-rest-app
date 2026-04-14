package com.screenrest.app.domain.usecase

import com.screenrest.app.data.local.datastore.MessageIndexDataStore
import com.screenrest.app.data.repository.AyahDatabaseRepository
import com.screenrest.app.data.repository.IslamicReminderRepository
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.DisplayMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRandomDisplayMessageUseCase @Inject constructor(
    private val ayahDatabaseRepository: AyahDatabaseRepository,
    private val settingsRepository: SettingsRepository,
    private val islamicReminderRepository: IslamicReminderRepository,
    private val messageIndexDataStore: MessageIndexDataStore
) {

    suspend operator fun invoke(): DisplayMessage? {
        val breakConfig = settingsRepository.breakConfig.first()
        val quranEnabled = breakConfig.quranMessagesEnabled
        val islamicEnabled = breakConfig.islamicRemindersEnabled

        // Ensure defaults are populated
        ayahDatabaseRepository.ensureDefaults()
        islamicReminderRepository.ensureDefaults()

        val ayahs = ayahDatabaseRepository.getAllAyahs().first()
        val reminders = islamicReminderRepository.getAllReminders().first()

        // Get last message type (0 = Reminder, 1 = Ayah)
        val lastMessageType = messageIndexDataStore.lastMessageType.first()

        // Sequential logic: alternate between Ayah and Reminder
        val shouldShowAyah = if (lastMessageType == 0) {
            // Last was Reminder, show Ayah if enabled
            quranEnabled && ayahs.isNotEmpty()
        } else {
            // Last was Ayah, show Reminder if enabled, otherwise Ayah again
            if (islamicEnabled && reminders.isNotEmpty()) {
                false // Show Reminder
            } else {
                quranEnabled && ayahs.isNotEmpty() // Show Ayah again
            }
        }

        return if (shouldShowAyah) {
            val ayahIndex = messageIndexDataStore.ayahIndex.first()
            val safeIndex = if (ayahs.isNotEmpty()) ayahIndex % ayahs.size else 0
            val nextIndex = (safeIndex + 1) % ayahs.size
            messageIndexDataStore.incrementAyahIndex(nextIndex)
            messageIndexDataStore.setLastMessageType(1)
            DisplayMessage.QuranAyah(ayahs[safeIndex])
        } else if (islamicEnabled && reminders.isNotEmpty()) {
            val reminderIndex = messageIndexDataStore.reminderIndex.first()
            val safeIndex = if (reminders.isNotEmpty()) reminderIndex % reminders.size else 0
            val nextIndex = (safeIndex + 1) % reminders.size
            messageIndexDataStore.incrementReminderIndex(nextIndex)
            messageIndexDataStore.setLastMessageType(0)
            DisplayMessage.IslamicReminder(reminders[safeIndex].text)
        } else if (quranEnabled && ayahs.isNotEmpty()) {
            val ayahIndex = messageIndexDataStore.ayahIndex.first()
            val safeIndex = if (ayahs.isNotEmpty()) ayahIndex % ayahs.size else 0
            val nextIndex = (safeIndex + 1) % ayahs.size
            messageIndexDataStore.incrementAyahIndex(nextIndex)
            messageIndexDataStore.setLastMessageType(1)
            DisplayMessage.QuranAyah(ayahs[safeIndex])
        } else if (reminders.isNotEmpty()) {
            // Absolute fallback: use any available reminder even if toggled off
            val reminderIndex = messageIndexDataStore.reminderIndex.first()
            val safeIndex = reminderIndex % reminders.size
            val nextIndex = (safeIndex + 1) % reminders.size
            messageIndexDataStore.incrementReminderIndex(nextIndex)
            DisplayMessage.IslamicReminder(reminders[safeIndex].text)
        } else if (ayahs.isNotEmpty()) {
            // Absolute fallback: use any available ayah even if toggled off
            val ayahIndex = messageIndexDataStore.ayahIndex.first()
            val safeIndex = ayahIndex % ayahs.size
            val nextIndex = (safeIndex + 1) % ayahs.size
            messageIndexDataStore.incrementAyahIndex(nextIndex)
            DisplayMessage.QuranAyah(ayahs[safeIndex])
        } else {
            // Truly nothing available - return null
            null
        }
    }
}
