package com.screenrest.app.presentation.settings.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.domain.model.IslamicReminder
import com.screenrest.app.domain.usecase.ManageIslamicRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IslamicRemindersViewModel @Inject constructor(
    private val manageIslamicRemindersUseCase: ManageIslamicRemindersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IslamicRemindersUiState())
    val uiState: StateFlow<IslamicRemindersUiState> = _uiState.asStateFlow()

    init {
        ensureDefaults()
        loadReminders()
    }

    private fun ensureDefaults() {
        viewModelScope.launch {
            manageIslamicRemindersUseCase.ensureDefaults()
        }
    }

    private fun loadReminders() {
        viewModelScope.launch {
            manageIslamicRemindersUseCase.getAllReminders().collect { reminders ->
                _uiState.value = _uiState.value.copy(reminders = reminders)
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            newReminderText = ""
        )
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            newReminderText = ""
        )
    }

    fun updateNewReminderText(text: String) {
        if (text.length <= 500) {
            _uiState.value = _uiState.value.copy(newReminderText = text)
        }
    }

    fun addReminder() {
        val text = _uiState.value.newReminderText.trim()
        if (text.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    manageIslamicRemindersUseCase.addReminder(text)
                    hideAddDialog()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            try {
                manageIslamicRemindersUseCase.deleteReminder(reminderId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class IslamicRemindersUiState(
    val reminders: List<IslamicReminder> = emptyList(),
    val showAddDialog: Boolean = false,
    val newReminderText: String = "",
    val error: String? = null
)
