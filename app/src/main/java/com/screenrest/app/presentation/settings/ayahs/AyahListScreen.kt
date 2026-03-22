package com.screenrest.app.presentation.settings.ayahs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenrest.app.domain.model.Ayah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahListScreen(
    viewModel: AyahListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showAddDialog) {
        AyahFormDialog(
            title = "Add Quranic Verse",
            confirmLabel = "Add",
            text = uiState.dialogText,
            surahName = uiState.dialogSurahName,
            surahNumber = uiState.dialogSurahNumber,
            ayahNumber = uiState.dialogAyahNumber,
            onTextChange = { viewModel.updateDialogText(it) },
            onSurahNameChange = { viewModel.updateDialogSurahName(it) },
            onSurahNumberChange = { viewModel.updateDialogSurahNumber(it) },
            onAyahNumberChange = { viewModel.updateDialogAyahNumber(it) },
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { viewModel.addAyah() }
        )
    }

    if (uiState.showEditDialog) {
        AyahFormDialog(
            title = "Edit Quranic Verse",
            confirmLabel = "Save",
            text = uiState.dialogText,
            surahName = uiState.dialogSurahName,
            surahNumber = uiState.dialogSurahNumber,
            ayahNumber = uiState.dialogAyahNumber,
            onTextChange = { viewModel.updateDialogText(it) },
            onSurahNameChange = { viewModel.updateDialogSurahName(it) },
            onSurahNumberChange = { viewModel.updateDialogSurahNumber(it) },
            onAyahNumberChange = { viewModel.updateDialogAyahNumber(it) },
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { viewModel.updateAyah() }
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quranic Verses") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Ayah"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "About Quranic Verses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "These verses from the Quran are shown during screen breaks. Tap a verse to edit it.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.ayahs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No Quranic Verses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tap the + button to add your first verse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.ayahs,
                        key = { it.id }
                    ) { ayah ->
                        AyahItem(
                            ayah = ayah,
                            onEdit = { viewModel.showEditDialog(ayah) },
                            onDelete = { viewModel.deleteAyah(ayah) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahItem(
    ayah: Ayah,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Ayah?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ayah.englishTranslation,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val reference = if (ayah.surahNumber > 0 && ayah.ayahNumber > 0) {
                    "${ayah.surahName} ${ayah.surahNumber}:${ayah.ayahNumber}"
                } else if (ayah.surahName.isNotBlank() && ayah.surahName != "Custom") {
                    ayah.surahName
                } else {
                    "Custom verse"
                }
                Text(
                    text = reference,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = { onEdit() }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Ayah",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Ayah",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AyahFormDialog(
    title: String,
    confirmLabel: String,
    text: String,
    surahName: String,
    surahNumber: String,
    ayahNumber: String,
    onTextChange: (String) -> Unit,
    onSurahNameChange: (String) -> Unit,
    onSurahNumberChange: (String) -> Unit,
    onAyahNumberChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text("Verse Translation") },
                    placeholder = { Text("Enter the English translation...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    supportingText = {
                        Text(
                            text = "${text.length}/500",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                OutlinedTextField(
                    value = surahName,
                    onValueChange = onSurahNameChange,
                    label = { Text("Surah Name (optional)") },
                    placeholder = { Text("e.g. Al-Baqarah") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = surahNumber,
                        onValueChange = onSurahNumberChange,
                        label = { Text("Surah #") },
                        placeholder = { Text("e.g. 2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = ayahNumber,
                        onValueChange = onAyahNumberChange,
                        label = { Text("Ayah #") },
                        placeholder = { Text("e.g. 255") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = text.trim().isNotEmpty()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
