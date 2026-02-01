package com.screenrest.app.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.EnforcementLevel
import com.screenrest.app.domain.model.PermissionStatus
import com.screenrest.app.domain.usecase.CheckPermissionsUseCase
import com.screenrest.app.service.ServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val checkPermissionsUseCase: CheckPermissionsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeState()
        refreshStatus()
    }
    
    private fun observeState() {
        viewModelScope.launch {
            combine(
                settingsRepository.breakConfig,
                settingsRepository.usageTrackingEnabled
            ) { config, enabled ->
                config to enabled
            }.collect { (config, enabled) ->
                _uiState.value = _uiState.value.copy(
                    breakConfig = config,
                    isServiceEnabled = enabled
                )
            }
        }
    }
    
    fun refreshStatus() {
        val permissions = checkPermissionsUseCase()
        val enforcementLevel = checkPermissionsUseCase.calculateEnforcementLevel(permissions)
        val isRunning = ServiceController.isRunning(context)
        
        _uiState.value = _uiState.value.copy(
            permissionStatus = permissions,
            enforcementLevel = enforcementLevel,
            isServiceRunning = isRunning
        )
    }
    
    fun toggleService() {
        viewModelScope.launch {
            val newState = !_uiState.value.isServiceEnabled
            settingsRepository.setUsageTrackingEnabled(newState)
            
            if (newState) {
                ServiceController.startTracking(context)
            } else {
                ServiceController.stopTracking(context)
            }
            
            refreshStatus()
        }
    }
}

data class HomeUiState(
    val breakConfig: BreakConfig = BreakConfig(),
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val enforcementLevel: EnforcementLevel = EnforcementLevel.NONE,
    val isServiceEnabled: Boolean = false,
    val isServiceRunning: Boolean = false
)
