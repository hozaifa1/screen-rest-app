package com.screenrest.app.presentation.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.domain.model.DisplayMessage
import com.screenrest.app.domain.usecase.GetRandomDisplayMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockState(
    val remainingSeconds: Int = 0,
    val displayMessage: DisplayMessage? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class BlockViewModel @Inject constructor(
    private val getRandomDisplayMessageUseCase: GetRandomDisplayMessageUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(BlockState())
    val state: StateFlow<BlockState> = _state.asStateFlow()
    
    private var onBlockComplete: (() -> Unit)? = null
    
    init {
        fetchDisplayMessage()
    }
    
    fun startCountdown(durationSeconds: Int, onComplete: () -> Unit) {
        onBlockComplete = onComplete
        _state.update { it.copy(remainingSeconds = durationSeconds) }
        
        viewModelScope.launch {
            repeat(durationSeconds) {
                delay(1000)
                _state.update { currentState ->
                    val newRemaining = currentState.remainingSeconds - 1
                    if (newRemaining <= 0) {
                        onBlockComplete?.invoke()
                    }
                    currentState.copy(remainingSeconds = newRemaining)
                }
            }
        }
    }
    
    private fun fetchDisplayMessage() {
        viewModelScope.launch {
            try {
                val message = getRandomDisplayMessageUseCase()
                _state.update { it.copy(displayMessage = message, isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        displayMessage = DisplayMessage.Custom("Take a moment to rest your eyes and reflect."),
                        isLoading = false
                    )
                }
            }
        }
    }
}
