package com.screenrest.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.EnforcementLevel
import com.screenrest.app.domain.model.PermissionStatus
import com.screenrest.app.domain.usecase.CheckPermissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val checkPermissionsUseCase: CheckPermissionsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    init {
        refreshPermissions()
    }
    
    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < 5) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep + 1)
        }
    }
    
    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = currentStep - 1)
        }
    }
    
    fun refreshPermissions() {
        val permissions = checkPermissionsUseCase()
        val enforcementLevel = checkPermissionsUseCase.calculateEnforcementLevel(permissions)
        _uiState.value = _uiState.value.copy(
            permissionStatus = permissions,
            enforcementLevel = enforcementLevel
        )
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            settingsRepository.setUsageTrackingEnabled(true)
        }
    }
}

data class OnboardingUiState(
    val currentStep: Int = 0,
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val enforcementLevel: EnforcementLevel = EnforcementLevel.NONE
)
