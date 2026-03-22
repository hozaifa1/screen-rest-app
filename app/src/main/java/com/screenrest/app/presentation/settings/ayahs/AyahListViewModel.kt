package com.screenrest.app.presentation.settings.ayahs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenrest.app.domain.model.Ayah
import com.screenrest.app.domain.usecase.ManageAyahsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AyahListUiState(
    val ayahs: List<Ayah> = emptyList(),
    val showAddDialog: Boolean = false,
    val newAyahText: String = "",
    val error: String? = null
)

@HiltViewModel
class AyahListViewModel @Inject constructor(
    private val manageAyahsUseCase: ManageAyahsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AyahListUiState())
    val uiState: StateFlow<AyahListUiState> = _uiState.asStateFlow()

    init {
        loadAyahs()
    }

    private fun loadAyahs() {
        viewModelScope.launch {
            manageAyahsUseCase.ensureDefaults()
            manageAyahsUseCase.getAllAyahs().collect { ayahs ->
                _uiState.update { it.copy(ayahs = ayahs) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, newAyahText = "") }
    }

    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false, newAyahText = "") }
    }

    fun updateNewAyahText(text: String) {
        if (text.length <= 500) {
            _uiState.update { it.copy(newAyahText = text) }
        }
    }

    fun addAyah() {
        val text = _uiState.value.newAyahText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(error = "Ayah text cannot be empty") }
            return
        }

        viewModelScope.launch {
            val ayah = Ayah(
                surahNumber = 0,
                ayahNumber = 0,
                arabicText = "",
                englishTranslation = text,
                surahName = "Custom"
            )
            
            val result = manageAyahsUseCase.addAyah(ayah)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(showAddDialog = false, newAyahText = "") }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Failed to add ayah") }
                }
            )
        }
    }

    fun deleteAyah(ayah: Ayah) {
        viewModelScope.launch {
            // Since we need the ID from the database, we'll need to track it differently
            // For now, we'll just show an error - this needs proper implementation
            _uiState.update { it.copy(error = "Delete functionality needs database ID tracking") }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
