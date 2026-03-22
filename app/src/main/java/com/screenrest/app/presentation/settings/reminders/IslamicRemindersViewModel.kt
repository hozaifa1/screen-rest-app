package com.screenrest.app.presentation.settings.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.domain.model.IslamicReminder
import com.screenrest.app.domain.usecase.ManageIslamicRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IslamicRemindersUiState(
    val reminders: List<IslamicReminder> = emptyList(),
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingReminder: IslamicReminder? = null,
    val dialogText: String = "",
    val error: String? = null
)

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
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                showEditDialog = false,
                editingReminder = null,
                dialogText = ""
            )
        }
    }

    fun showEditDialog(reminder: IslamicReminder) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                showAddDialog = false,
                editingReminder = reminder,
                dialogText = reminder.text
            )
        }
    }

    fun hideDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = false,
                showEditDialog = false,
                editingReminder = null,
                dialogText = ""
            )
        }
    }

    fun updateDialogText(text: String) {
        if (text.length <= 500) {
            _uiState.update { it.copy(dialogText = text) }
        }
    }

    fun addReminder() {
        val text = _uiState.value.dialogText.trim()
        if (text.isNotEmpty()) {
            viewModelScope.launch {
                val result = manageIslamicRemindersUseCase.addReminder(text)
                result.fold(
                    onSuccess = { hideDialog() },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to add reminder") }
                    }
                )
            }
        }
    }

    fun updateReminder() {
        val state = _uiState.value
        val editing = state.editingReminder ?: return
        val text = state.dialogText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(error = "Reminder text cannot be empty") }
            return
        }

        viewModelScope.launch {
            val result = manageIslamicRemindersUseCase.updateReminder(editing.id, text)
            result.fold(
                onSuccess = { hideDialog() },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to update reminder") }
                }
            )
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            val result = manageIslamicRemindersUseCase.deleteReminder(reminderId)
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to delete reminder") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
