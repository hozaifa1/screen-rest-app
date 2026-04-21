package com.screenrest.app.presentation.blocktime

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.BlockTimeRepository
import com.screenrest.app.domain.model.BlockTimeProfile
import com.screenrest.app.service.ServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class BlockTimeEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockTimeRepository: BlockTimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockTimeEditorUiState())
    val uiState: StateFlow<BlockTimeEditorUiState> = _uiState.asStateFlow()

    private var originalProfile: BlockTimeProfile? = null
    private var profileLoaded = false

    fun loadProfile(profileId: Long) {
        if (profileLoaded) return
        profileLoaded = true
        
        if (profileId == -1L) {
            // New profile with defaults
            _uiState.value = BlockTimeEditorUiState(
                startMinuteOfDay = 12 * 60, // 12:00 PM
                endMinuteOfDay = 13 * 60,   // 1:00 PM
                daysOfWeek = setOf(1, 2, 3, 4, 5) // Mon-Fri
            )
            return
        }

        viewModelScope.launch {
            val profile = blockTimeRepository.getProfileById(profileId)
            if (profile != null) {
                originalProfile = profile
                _uiState.value = BlockTimeEditorUiState(
                    id = profile.id,
                    name = profile.name,
                    startMinuteOfDay = profile.startMinuteOfDay,
                    endMinuteOfDay = profile.endMinuteOfDay,
                    daysOfWeek = profile.daysOfWeek,
                    customMessage = profile.customMessage,
                    isPasswordLocked = profile.isPasswordLocked,
                    isTimeLocked = profile.isTimeLocked,
                    isLocked = profile.isLocked,
                    originalEndMinuteOfDay = profile.endMinuteOfDay,
                    originalDaysOfWeek = profile.daysOfWeek,
                    passwordHash = profile.passwordHash,
                    timeLockUntilMillis = profile.timeLockUntilMillis
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateStartTime(minute: Int) {
        if (!_uiState.value.isLocked) {
            _uiState.value = _uiState.value.copy(startMinuteOfDay = minute)
        }
    }

    fun updateEndTime(minute: Int) {
        val state = _uiState.value
        // When locked, only allow extending (later end time)
        if (state.isLocked && minute < state.originalEndMinuteOfDay) {
            return
        }
        _uiState.value = state.copy(endMinuteOfDay = minute)
    }

    fun toggleDay(day: Int) {
        val state = _uiState.value
        val currentDays = state.daysOfWeek.toMutableSet()
        
        if (day in currentDays) {
            // Only allow removing if not locked or not an original day
            if (!state.isLocked || day !in state.originalDaysOfWeek) {
                currentDays.remove(day)
            }
        } else {
            currentDays.add(day)
        }
        
        _uiState.value = state.copy(daysOfWeek = currentDays)
    }

    fun updateCustomMessage(message: String?) {
        if (!_uiState.value.isLocked) {
            _uiState.value = _uiState.value.copy(customMessage = message)
        }
    }

    fun setPassword(password: String) {
        val hash = hashPassword(password)
        _uiState.value = _uiState.value.copy(
            isPasswordLocked = true,
            isLocked = true,
            passwordHash = hash
        )
        saveProfile()
    }

    fun setTimeLock(durationMs: Long) {
        val lockUntil = System.currentTimeMillis() + durationMs
        _uiState.value = _uiState.value.copy(
            isTimeLocked = true,
            isLocked = true,
            timeLockUntilMillis = lockUntil
        )
        saveProfile()
    }

    /** Extend a time lock by the given duration (can only extend, never reduce) */
    fun extendTimeLock(additionalMs: Long) {
        val state = _uiState.value
        val currentEnd = state.timeLockUntilMillis.coerceAtLeast(System.currentTimeMillis())
        val newEnd = currentEnd + additionalMs
        _uiState.value = state.copy(
            isTimeLocked = true,
            isLocked = true,
            timeLockUntilMillis = newEnd
        )
        saveProfile()
    }

    /** Clear a password lock — requires password verification before calling */
    fun clearPasswordLock() {
        val state = _uiState.value
        if (!state.isPasswordLocked) return
        _uiState.value = state.copy(
            isPasswordLocked = false,
            passwordHash = null,
            // Only unlock if there's no time lock either
            isLocked = state.isTimeLocked
        )
        saveProfile()
    }

    fun saveProfile() {
        val state = _uiState.value
        val profile = BlockTimeProfile(
            id = state.id,
            name = state.name,
            startMinuteOfDay = state.startMinuteOfDay,
            endMinuteOfDay = state.endMinuteOfDay,
            daysOfWeek = state.daysOfWeek,
            isEnabled = originalProfile?.isEnabled ?: false,
            customMessage = state.customMessage,
            createdAt = originalProfile?.createdAt ?: System.currentTimeMillis(),
            passwordHash = state.passwordHash,
            timeLockUntilMillis = state.timeLockUntilMillis
        )

        viewModelScope.launch {
            blockTimeRepository.saveProfile(profile)
            restartScheduler()
        }
    }

    fun deleteProfile() {
        val state = _uiState.value
        if (state.id == 0L || state.isLocked) return

        val profile = BlockTimeProfile(
            id = state.id,
            name = state.name,
            startMinuteOfDay = state.startMinuteOfDay,
            endMinuteOfDay = state.endMinuteOfDay,
            daysOfWeek = state.daysOfWeek
        )

        viewModelScope.launch {
            blockTimeRepository.deleteProfile(profile)
            restartScheduler()
        }
    }

    private fun restartScheduler() {
        try {
            ServiceController.startBlockTimeScheduler(context)
        } catch (e: Exception) {
            // Non-fatal
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

data class BlockTimeEditorUiState(
    val id: Long = 0,
    val name: String = "",
    val startMinuteOfDay: Int = 720, // 12:00 PM
    val endMinuteOfDay: Int = 780,   // 1:00 PM
    val daysOfWeek: Set<Int> = emptySet(),
    val customMessage: String? = null,
    val isPasswordLocked: Boolean = false,
    val isTimeLocked: Boolean = false,
    val isLocked: Boolean = false,
    val originalEndMinuteOfDay: Int = 0,
    val originalDaysOfWeek: Set<Int> = emptySet(),
    val passwordHash: String? = null,
    val timeLockUntilMillis: Long = 0
)
