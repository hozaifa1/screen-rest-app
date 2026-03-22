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
    val showEditDialog: Boolean = false,
    val editingAyah: Ayah? = null,
    val dialogText: String = "",
    val dialogSurahName: String = "",
    val dialogSurahNumber: String = "",
    val dialogAyahNumber: String = "",
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
        _uiState.update {
            it.copy(
                showAddDialog = true,
                showEditDialog = false,
                editingAyah = null,
                dialogText = "",
                dialogSurahName = "",
                dialogSurahNumber = "",
                dialogAyahNumber = ""
            )
        }
    }

    fun showEditDialog(ayah: Ayah) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                showAddDialog = false,
                editingAyah = ayah,
                dialogText = ayah.englishTranslation,
                dialogSurahName = ayah.surahName,
                dialogSurahNumber = if (ayah.surahNumber > 0) ayah.surahNumber.toString() else "",
                dialogAyahNumber = if (ayah.ayahNumber > 0) ayah.ayahNumber.toString() else ""
            )
        }
    }

    fun hideDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = false,
                showEditDialog = false,
                editingAyah = null,
                dialogText = "",
                dialogSurahName = "",
                dialogSurahNumber = "",
                dialogAyahNumber = ""
            )
        }
    }

    fun updateDialogText(text: String) {
        if (text.length <= 500) {
            _uiState.update { it.copy(dialogText = text) }
        }
    }

    fun updateDialogSurahName(name: String) {
        _uiState.update { it.copy(dialogSurahName = name) }
    }

    fun updateDialogSurahNumber(number: String) {
        val filtered = number.filter { it.isDigit() }
        _uiState.update { it.copy(dialogSurahNumber = filtered) }
    }

    fun updateDialogAyahNumber(number: String) {
        val filtered = number.filter { it.isDigit() }
        _uiState.update { it.copy(dialogAyahNumber = filtered) }
    }

    fun addAyah() {
        val state = _uiState.value
        val text = state.dialogText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(error = "Verse translation cannot be empty") }
            return
        }

        viewModelScope.launch {
            val ayah = Ayah(
                surahNumber = state.dialogSurahNumber.toIntOrNull() ?: 0,
                ayahNumber = state.dialogAyahNumber.toIntOrNull() ?: 0,
                arabicText = "",
                englishTranslation = text,
                surahName = state.dialogSurahName.trim().ifEmpty { "Custom" }
            )

            val result = manageAyahsUseCase.addAyah(ayah)
            result.fold(
                onSuccess = { hideDialog() },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Failed to add ayah") }
                }
            )
        }
    }

    fun updateAyah() {
        val state = _uiState.value
        val editing = state.editingAyah ?: return
        val text = state.dialogText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(error = "Verse translation cannot be empty") }
            return
        }

        viewModelScope.launch {
            val updated = editing.copy(
                englishTranslation = text,
                surahName = state.dialogSurahName.trim().ifEmpty { "Custom" },
                surahNumber = state.dialogSurahNumber.toIntOrNull() ?: 0,
                ayahNumber = state.dialogAyahNumber.toIntOrNull() ?: 0
            )

            val result = manageAyahsUseCase.updateAyah(updated)
            result.fold(
                onSuccess = { hideDialog() },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Failed to update ayah") }
                }
            )
        }
    }

    fun deleteAyah(ayah: Ayah) {
        viewModelScope.launch {
            val result = manageAyahsUseCase.deleteAyah(ayah.id)
            result.onFailure { exception ->
                _uiState.update { it.copy(error = exception.message ?: "Failed to delete ayah") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
