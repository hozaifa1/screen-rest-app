package com.screenrest.app.presentation.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.BlockTimeRepository
import com.screenrest.app.data.repository.SettingsRepository
import com.screenrest.app.domain.model.BlockTimeProfile
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.EnforcementLevel
import com.screenrest.app.domain.model.PermissionStatus
import com.screenrest.app.domain.usecase.CheckPermissionsUseCase
import com.screenrest.app.service.ServiceController
import com.screenrest.app.service.UsageTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val blockTimeRepository: BlockTimeRepository,
    private val checkPermissionsUseCase: CheckPermissionsUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    init {
        observeState()
        refreshStatus()
        startTimer()
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

        viewModelScope.launch {
            blockTimeRepository.getEnabledProfiles().collect { profiles ->
                val summary = computeNextBlockSummary(profiles)
                _uiState.value = _uiState.value.copy(
                    nextBlockSummary = summary,
                    activeBlockProfiles = profiles.size
                )
            }
        }
    }

    private fun computeNextBlockSummary(profiles: List<BlockTimeProfile>): String {
        if (profiles.isEmpty()) return "No scheduled blocks"

        val now = java.time.LocalTime.now()
        val today = java.time.LocalDate.now().dayOfWeek.value
        val nowMinutes = now.hour * 60 + now.minute

        // Check if any block is currently active
        val activeProfile = profiles.firstOrNull { isWithinBlock(it, nowMinutes, today) }
        if (activeProfile != null) {
            val endFormatted = formatMinuteOfDay(activeProfile.endMinuteOfDay)
            return "Active until $endFormatted"
        }

        // Find next upcoming block today
        val nextToday = profiles
            .filter { today in it.daysOfWeek && it.startMinuteOfDay > nowMinutes }
            .minByOrNull { it.startMinuteOfDay }

        if (nextToday != null) {
            val startFormatted = formatMinuteOfDay(nextToday.startMinuteOfDay)
            val endFormatted = formatMinuteOfDay(nextToday.endMinuteOfDay)
            return "Next: Today $startFormatted – $endFormatted"
        }

        return "No upcoming blocks today"
    }

    private fun isWithinBlock(profile: BlockTimeProfile, nowMinutes: Int, dayOfWeek: Int): Boolean {
        if (dayOfWeek !in profile.daysOfWeek) {
            val yesterday = if (dayOfWeek == 1) 7 else dayOfWeek - 1
            if (yesterday !in profile.daysOfWeek) return false
            if (profile.endMinuteOfDay <= profile.startMinuteOfDay) {
                return nowMinutes < profile.endMinuteOfDay
            }
            return false
        }

        return if (profile.endMinuteOfDay >= profile.startMinuteOfDay) {
            nowMinutes in profile.startMinuteOfDay until profile.endMinuteOfDay
        } else {
            nowMinutes >= profile.startMinuteOfDay || nowMinutes < profile.endMinuteOfDay
        }
    }

    private fun formatMinuteOfDay(minute: Int): String {
        val h = minute / 60
        val m = minute % 60
        val amPm = if (h < 12) "AM" else "PM"
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format("%d:%02d %s", hour12, m, amPm)
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            Log.d(TAG, "Timer started")
            while (true) {
                try {
                    updateTimerDisplay()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating timer", e)
                }
                delay(1000L)
            }
        }
    }
    
    private fun updateTimerDisplay() {
        val config = _uiState.value.breakConfig
        val thresholdMs = config.usageThresholdSeconds * 1000L
        val usageMs = UsageTrackingService.currentUsageMs
        val remainingMs = (thresholdMs - usageMs).coerceAtLeast(0L)
        
        _uiState.value = _uiState.value.copy(
            usedTimeMinutes = (usageMs / 60000).toInt(),
            usedTimeSeconds = ((usageMs % 60000) / 1000).toInt(),
            remainingTimeMinutes = (remainingMs / 60000).toInt(),
            remainingTimeSeconds = ((remainingMs % 60000) / 1000).toInt()
        )
    }
    
    fun refreshStatus() {
        try {
            val permissions = checkPermissionsUseCase()
            val enforcementLevel = checkPermissionsUseCase.calculateEnforcementLevel(permissions)
            val isRunning = ServiceController.isRunning(context)
            
            _uiState.value = _uiState.value.copy(
                permissionStatus = permissions,
                enforcementLevel = enforcementLevel,
                isServiceRunning = isRunning
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing status", e)
        }
    }
    
    private var isToggling = false
    
    fun toggleService() {
        if (isToggling) return
        isToggling = true
        
        viewModelScope.launch {
            try {
                val shouldStart = !_uiState.value.isServiceRunning
                settingsRepository.setUsageTrackingEnabled(shouldStart)
                
                if (shouldStart) {
                    ServiceController.startTracking(context)
                } else {
                    ServiceController.stopTracking(context)
                }
                
                _uiState.value = _uiState.value.copy(
                    isServiceEnabled = shouldStart,
                    isServiceRunning = shouldStart
                )
                
                delay(1500L)
                refreshStatus()
            } finally {
                isToggling = false
            }
        }
    }
}

data class HomeUiState(
    val breakConfig: BreakConfig = BreakConfig(),
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val enforcementLevel: EnforcementLevel = EnforcementLevel.NONE,
    val isServiceEnabled: Boolean = false,
    val isServiceRunning: Boolean = false,
    val usedTimeMinutes: Int = 0,
    val usedTimeSeconds: Int = 0,
    val remainingTimeMinutes: Int = 0,
    val remainingTimeSeconds: Int = 0,
    val nextBlockSummary: String = "No scheduled blocks",
    val activeBlockProfiles: Int = 0
)
