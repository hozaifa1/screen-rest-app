package com.screenrest.app.presentation.blocktime

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.data.repository.BlockTimeRepository
import com.screenrest.app.domain.model.BlockTimeProfile
import com.screenrest.app.service.ServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class BlockTimeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockTimeRepository: BlockTimeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val profiles: StateFlow<List<BlockTimeProfile>> = blockTimeRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextBlockSummary: StateFlow<String> = blockTimeRepository.getEnabledProfiles()
        .map { profiles -> computeNextBlockSummary(profiles) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "No scheduled blocks")

    fun saveProfile(profile: BlockTimeProfile) {
        viewModelScope.launch {
            blockTimeRepository.saveProfile(profile)
            restartScheduler()
        }
    }

    fun deleteProfile(profile: BlockTimeProfile) {
        viewModelScope.launch {
            blockTimeRepository.deleteProfile(profile)
            restartScheduler()
        }
    }

    fun toggleProfileEnabled(profile: BlockTimeProfile) {
        // Locked profiles cannot be disabled
        if (profile.isLocked && profile.isEnabled) return
        viewModelScope.launch {
            blockTimeRepository.saveProfile(profile.copy(isEnabled = !profile.isEnabled))
            restartScheduler()
        }
    }

    private fun restartScheduler() {
        try {
            ServiceController.startBlockTimeScheduler(context)
        } catch (e: Exception) {
            // Non-fatal: scheduler will pick up changes on next cycle
        }
    }

    private fun computeNextBlockSummary(profiles: List<BlockTimeProfile>): String {
        if (profiles.isEmpty()) return "No scheduled blocks"

        val now = LocalTime.now()
        val today = LocalDate.now().dayOfWeek.value // 1=Mon..7=Sun
        val nowMinutes = now.hour * 60 + now.minute

        // Check if any block is currently active
        val activeProfile = profiles.firstOrNull { isWithinBlock(it, nowMinutes, today) }
        if (activeProfile != null) {
            val endFormatted = formatMinuteOfDay(activeProfile.endMinuteOfDay)
            return "Active until $endFormatted"
        }

        // Find next upcoming block using minutes-from-now for all candidates
        var nextProfile: BlockTimeProfile? = null
        var bestMinutesFromNow = Int.MAX_VALUE
        var nextIsToday = false

        for (profile in profiles) {
            // Check today first
            if (today in profile.daysOfWeek && profile.startMinuteOfDay > nowMinutes) {
                val minutesAway = profile.startMinuteOfDay - nowMinutes
                if (minutesAway < bestMinutesFromNow) {
                    nextProfile = profile
                    bestMinutesFromNow = minutesAway
                    nextIsToday = true
                }
            }

            // Check future days (tomorrow through next week)
            for (d in 1..7) {
                val checkDay = ((today - 1 + d) % 7) + 1
                if (checkDay in profile.daysOfWeek) {
                    val minutesAway = (d * 1440) + profile.startMinuteOfDay - nowMinutes
                    if (minutesAway < bestMinutesFromNow) {
                        nextProfile = profile
                        bestMinutesFromNow = minutesAway
                        nextIsToday = false
                    }
                    break
                }
            }
        }

        return if (nextProfile != null && nextIsToday) {
            val startFormatted = formatMinuteOfDay(nextProfile.startMinuteOfDay)
            val endFormatted = formatMinuteOfDay(nextProfile.endMinuteOfDay)
            "Next: Today $startFormatted – $endFormatted"
        } else if (nextProfile != null) {
            val startFormatted = formatMinuteOfDay(nextProfile.startMinuteOfDay)
            "Next: $startFormatted"
        } else {
            "No upcoming blocks"
        }
    }

    private fun isWithinBlock(profile: BlockTimeProfile, nowMinutes: Int, dayOfWeek: Int): Boolean {
        if (dayOfWeek !in profile.daysOfWeek) {
            // Check yesterday's cross-midnight block
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
            // Cross-midnight
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
}
