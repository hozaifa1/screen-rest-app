package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.AyahRepository
import com.screenrest.app.data.repository.CustomMessageRepository
import com.screenrest.app.domain.model.DisplayMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class GetRandomDisplayMessageUseCase @Inject constructor(
    private val customMessageRepository: CustomMessageRepository,
    private val ayahRepository: AyahRepository
) {
    suspend operator fun invoke(): DisplayMessage {
        val customMessages = customMessageRepository.getAllMessages().first()
        
        return when {
            customMessages.isNotEmpty() && Random.nextBoolean() -> {
                val randomMessage = customMessages.random()
                DisplayMessage.Custom(randomMessage.text)
            }
            else -> {
                val ayahResult = ayahRepository.getRandomAyah()
                ayahResult.fold(
                    onSuccess = { ayah -> DisplayMessage.QuranAyah(ayah) },
                    onFailure = {
                        DisplayMessage.Custom("Take a moment to rest your eyes and reflect.")
                    }
                )
            }
        }
    }
}
