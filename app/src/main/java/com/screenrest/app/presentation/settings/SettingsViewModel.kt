package com.screenrest.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.ThemeColor
import com.screenrest.app.domain.model.ThemeMode
import com.screenrest.app.domain.usecase.UpdateBreakConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateBreakConfigUseCase: UpdateBreakConfigUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        observeSettings()
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.breakConfig,
                settingsRepository.themeMode,
                settingsRepository.themeColor,
                settingsRepository.whitelistApps
            ) { config, theme, color, whitelist ->
                SettingsData(config, theme, color, whitelist)
            }.collect { data ->
                _uiState.value = _uiState.value.copy(
                    breakConfig = data.config,
                    themeMode = data.theme,
                    themeColor = data.color,
                    whitelistApps = data.whitelist
                )
            }
        }
    }
    
    private data class SettingsData(
        val config: BreakConfig,
        val theme: ThemeMode,
        val color: ThemeColor,
        val whitelist: Set<String>
    )
    
    fun updateTimers(thresholdSeconds: Int, durationSeconds: Int) {
        viewModelScope.launch {
            val config = _uiState.value.breakConfig.copy(
                usageThresholdSeconds = thresholdSeconds,
                blockDurationSeconds = durationSeconds
            )
            
            if (durationSeconds > 120) {
                _uiState.value = _uiState.value.copy(showLongDurationWarning = true)
            }
            if (thresholdSeconds < 30) {
                _uiState.value = _uiState.value.copy(showShortThresholdWarning = true)
            }
            
            updateBreakConfigUseCase(config)
        }
    }
    
    fun updateTheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(theme)
        }
    }
    
    fun updateThemeColor(color: ThemeColor) {
        viewModelScope.launch {
            settingsRepository.updateThemeColor(color)
        }
    }
    
    fun updateQuranMessagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val config = _uiState.value.breakConfig.copy(quranMessagesEnabled = enabled)
            updateBreakConfigUseCase(config)
        }
    }
    
    fun updateIslamicRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val config = _uiState.value.breakConfig.copy(islamicRemindersEnabled = enabled)
            updateBreakConfigUseCase(config)
        }
    }
    
    fun dismissLongDurationWarning() {
        _uiState.value = _uiState.value.copy(showLongDurationWarning = false)
    }
    
    fun dismissShortThresholdWarning() {
        _uiState.value = _uiState.value.copy(showShortThresholdWarning = false)
    }
    
    fun toggleWhitelistApp(packageName: String) {
        viewModelScope.launch {
            val currentWhitelist = _uiState.value.whitelistApps
            if (currentWhitelist.contains(packageName)) {
                settingsRepository.removeWhitelistApp(packageName)
            } else {
                settingsRepository.addWhitelistApp(packageName)
            }
        }
    }
}

data class SettingsUiState(
    val breakConfig: BreakConfig = BreakConfig(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: ThemeColor = ThemeColor.TEAL,
    val whitelistApps: Set<String> = emptySet(),
    val showLongDurationWarning: Boolean = false,
    val showShortThresholdWarning: Boolean = false
)
