package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.AyahRepository
import com.screenrest.app.data.repository.CustomMessageRepository
import com.screenrest.app.data.repository.IslamicReminderRepository
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.DisplayMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRandomDisplayMessageUseCase @Inject constructor(
    private val customMessageRepository: CustomMessageRepository,
    private val ayahRepository: AyahRepository,
    private val settingsRepository: SettingsRepository,
    private val islamicReminderRepository: IslamicReminderRepository
) {

    suspend operator fun invoke(): DisplayMessage {
        val customMessages = customMessageRepository.getAllMessages().first()
        val breakConfig = settingsRepository.breakConfig.first()
        val quranEnabled = breakConfig.quranMessagesEnabled
        val islamicEnabled = breakConfig.islamicRemindersEnabled
        
        // Ensure defaults are populated
        islamicReminderRepository.ensureDefaults()
        val islamicReminders = islamicReminderRepository.getAllReminders().first()
        
        // Build pool of enabled source types
        val sourceTypes = mutableListOf<String>()
        
        if (quranEnabled) sourceTypes.add("quran")
        if (islamicEnabled && islamicReminders.isNotEmpty()) sourceTypes.add("islamic")
        if (customMessages.isNotEmpty()) sourceTypes.add("custom")
        
        if (sourceTypes.isEmpty()) {
            return DisplayMessage.Custom("Take a moment to rest your eyes and reflect.")
        }
        
        return when (sourceTypes.random()) {
            "quran" -> {
                val ayahResult = ayahRepository.getRandomAyah()
                ayahResult.fold(
                    onSuccess = { ayah -> DisplayMessage.QuranAyah(ayah) },
                    onFailure = { DisplayMessage.Custom("Take a moment to rest your eyes and reflect.") }
                )
            }
            "islamic" -> DisplayMessage.IslamicReminder(islamicReminders.random().text)
            "custom" -> DisplayMessage.Custom(customMessages.random().text)
            else -> DisplayMessage.Custom("Take a moment to rest your eyes and reflect.")
        }
    }
}
