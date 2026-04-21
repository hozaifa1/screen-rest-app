package com.screenrest.app.presentation.blocktime

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.BlockTimeRepository
import com.screenrest.app.service.BlockOverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class QuickBlockUiState(
    val selectedMode: QuickBlockMode = QuickBlockMode.DURATION,
    val hours: Int = 0,
    val minutes: Int = 30,
    val endTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0),
    val endDate: LocalDate = LocalDate.now(),
    val isActive: Boolean = false,
    val activeBlockDurationMinutes: Int? = null,
    val hasConflict: Boolean = false,
    val conflictMessage: String? = null
)

enum class QuickBlockMode {
    DURATION,
    END_TIME
}

sealed class QuickBlockEvent {
    data object BlockStarted : QuickBlockEvent()
    data class Error(val message: String) : QuickBlockEvent()
}

@HiltViewModel
class QuickBlockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockTimeRepository: BlockTimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickBlockUiState())
    val uiState: StateFlow<QuickBlockUiState> = _uiState.asStateFlow()

    private var event: QuickBlockEvent? = null

    init {
        checkForConflicts()
    }

    fun setMode(mode: QuickBlockMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun setHours(hours: Int) {
        _uiState.value = _uiState.value.copy(hours = hours.coerceIn(0, 23))
    }

    fun setMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(minutes = minutes.coerceIn(0, 59))
    }

    fun setEndTime(time: LocalTime) {
        _uiState.value = _uiState.value.copy(endTime = time)
    }

    fun setEndDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(endDate = date)
    }

    fun startQuickBlock() {
        viewModelScope.launch {
            val state = _uiState.value

            // Calculate duration based on mode
            val endDateTime = LocalDateTime.of(state.endDate, state.endTime)
            val now = LocalDateTime.now()

            val durationSeconds = if (state.selectedMode == QuickBlockMode.DURATION) {
                (state.hours * 3600 + state.minutes * 60).toLong()
            } else {
                val durationMs = java.time.Duration.between(now, endDateTime).toMillis()
                if (durationMs <= 0) {
                    event = QuickBlockEvent.Error("End time must be in the future")
                    return@launch
                }
                (durationMs / 1000).toLong()
            }

            if (durationSeconds <= 0) {
                event = QuickBlockEvent.Error("Duration must be greater than 0")
                return@launch
            }

            if (durationSeconds > 24 * 3600) {
                event = QuickBlockEvent.Error("Maximum duration is 24 hours")
                return@launch
            }

            val endTimeMs = System.currentTimeMillis() + (durationSeconds * 1000)

            // Start the overlay immediately
            val intent = Intent(context, BlockOverlayService::class.java).apply {
                putExtra(BlockOverlayService.EXTRA_DURATION_SECONDS, durationSeconds.toInt())
                putExtra(BlockOverlayService.EXTRA_BLOCK_MODE, BlockOverlayService.MODE_SCHEDULED)
                putExtra(BlockOverlayService.EXTRA_END_TIME_MS, endTimeMs)
            }
            context.startService(intent)

            event = QuickBlockEvent.BlockStarted
            _uiState.value = _uiState.value.copy(isActive = true, activeBlockDurationMinutes = (durationSeconds / 60).toInt())
        }
    }

    fun checkForConflicts() {
        viewModelScope.launch {
            // Check if there's an active profile block
            val enabledProfiles = blockTimeRepository.getEnabledProfilesOnce()
            val now = java.time.LocalTime.now()
            val today = java.time.LocalDate.now().dayOfWeek.value
            val nowMinutes = now.hour * 60 + now.minute

            val activeProfile = enabledProfiles.firstOrNull { profile ->
                val days = profile.daysOfWeek
                val withinTime = if (profile.endMinuteOfDay >= profile.startMinuteOfDay) {
                    nowMinutes in profile.startMinuteOfDay until profile.endMinuteOfDay
                } else {
                    nowMinutes >= profile.startMinuteOfDay || nowMinutes < profile.endMinuteOfDay
                }
                today in days && withinTime
            }

            if (activeProfile != null) {
                _uiState.value = _uiState.value.copy(
                    hasConflict = true,
                    conflictMessage = "A profile block (${activeProfile.name}) is currently active. Starting a quick block will override it."
                )
            } else {
                _uiState.value = _uiState.value.copy(hasConflict = false, conflictMessage = null)
            }
        }
    }

    fun popEvent(): QuickBlockEvent? {
        val e = event
        event = null
        return e
    }
}