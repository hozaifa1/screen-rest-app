package com.screenrest.app.presentation.settings.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.domain.model.CustomMessage
import com.screenrest.app.domain.usecase.ManageCustomMessagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomMessagesViewModel @Inject constructor(
    private val manageCustomMessagesUseCase: ManageCustomMessagesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CustomMessagesUiState())
    val uiState: StateFlow<CustomMessagesUiState> = _uiState.asStateFlow()
    
    init {
        loadMessages()
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            manageCustomMessagesUseCase.getAllMessages().collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }
    
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            newMessageText = ""
        )
    }
    
    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            newMessageText = ""
        )
    }
    
    fun updateNewMessageText(text: String) {
        if (text.length <= 500) {
            _uiState.value = _uiState.value.copy(newMessageText = text)
        }
    }
    
    fun addMessage() {
        val text = _uiState.value.newMessageText.trim()
        if (text.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    manageCustomMessagesUseCase.addMessage(text)
                    hideAddDialog()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }
    }
    
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                manageCustomMessagesUseCase.deleteMessage(messageId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CustomMessagesUiState(
    val messages: List<CustomMessage> = emptyList(),
    val showAddDialog: Boolean = false,
    val newMessageText: String = "",
    val error: String? = null
)
