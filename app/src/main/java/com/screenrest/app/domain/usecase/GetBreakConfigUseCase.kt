package com.screenrest.app.domain.usecase

import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBreakConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<BreakConfig> {
        return settingsRepository.breakConfig
    }
}
