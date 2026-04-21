package com.screenrest.app.presentation.blocktime

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockTimeProfileEditorScreen(
    profileId: Long,
    viewModel: BlockTimeEditorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (profileId == -1L) "New Profile" else "Edit Profile")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveProfile()
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Profile Name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isLocked
            )

            // Time Selection
            TimeSelectionSection(
                startMinute = uiState.startMinuteOfDay,
                endMinute = uiState.endMinuteOfDay,
                isLocked = uiState.isLocked,
                originalEndMinute = uiState.originalEndMinuteOfDay,
                onStartChange = { viewModel.updateStartTime(it) },
                onEndChange = { viewModel.updateEndTime(it) }
            )

            // Days of Week
            DaysOfWeekSection(
                selectedDays = uiState.daysOfWeek,
                isLocked = uiState.isLocked,
                originalDays = uiState.originalDaysOfWeek,
                onDayToggle = { viewModel.toggleDay(it) }
            )

            // Custom Message
            OutlinedTextField(
                value = uiState.customMessage ?: "",
                onValueChange = { viewModel.updateCustomMessage(it.ifEmpty { null }) },
                label = { Text("Custom Message (optional)") },
                placeholder = { Text("Leave empty for random Quran verse") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !uiState.isLocked
            )

            // Lock Settings
            if (profileId != -1L) {
                LockSettingsSection(
                    isPasswordLocked = uiState.isPasswordLocked,
                    isTimeLocked = uiState.isTimeLocked,
                    timeLockUntilMillis = uiState.timeLockUntilMillis,
                    onSetPassword = { password ->
                        viewModel.setPassword(password)
                        scope.launch {
                            snackbarHostState.showSnackbar("Password lock set")
                        }
                    },
                    onSetTimeLock = { durationMs ->
                        viewModel.setTimeLock(durationMs)
                        scope.launch {
                            snackbarHostState.showSnackbar("Time lock set")
                        }
                    },
                    onExtendTimeLock = { durationMs ->
                        viewModel.extendTimeLock(durationMs)
                        scope.launch {
                            snackbarHostState.showSnackbar("Time lock extended")
                        }
                    },
                    onVerifyAndClearPassword = { password ->
                        // Verification is done at the ViewModel layer via hash comparison
                        val state = uiState
                        val hash = java.security.MessageDigest.getInstance("SHA-256")
                            .digest(password.toByteArray())
                            .joinToString("") { "%02x".format(it) }
                        if (hash == state.passwordHash) {
                            viewModel.clearPasswordLock()
                            scope.launch {
                                snackbarHostState.showSnackbar("Password lock removed")
                            }
                            true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Incorrect password")
                            }
                            false
                        }
                    }
                )
            }

            // Delete Button
            if (profileId != -1L && !uiState.isLocked) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.deleteProfile()
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Profile")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectionSection(
    startMinute: Int,
    endMinute: Int,
    isLocked: Boolean,
    originalEndMinute: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Time Range",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeButton(
                    label = "Start",
                    minute = startMinute,
                    enabled = !isLocked,
                    onClick = { showStartPicker = true }
                )
                TimeButton(
                    label = "End",
                    minute = endMinute,
                    enabled = true, // Always allow extending end time
                    onClick = { showEndPicker = true }
                )
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialMinute = startMinute,
            onDismiss = { showStartPicker = false },
            onConfirm = { minute ->
                onStartChange(minute)
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            initialMinute = endMinute,
            minMinute = if (isLocked) originalEndMinute else null,
            onDismiss = { showEndPicker = false },
            onConfirm = { minute ->
                onEndChange(minute)
                showEndPicker = false
            }
        )
    }
}

@Composable
private fun TimeButton(
    label: String,
    minute: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Text(formatMinuteOfDay(minute))
        }
    }
}

@Composable
private fun DaysOfWeekSection(
    selectedDays: Set<Int>,
    isLocked: Boolean,
    originalDays: Set<Int>,
    onDayToggle: (Int) -> Unit
) {
    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Days of Week",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (day in 1..7) {
                    val isSelected = day in selectedDays
                    val isOriginal = day in originalDays
                    // When locked: original selected days are frozen (disabled), 
                    // all other days remain interactive (can add new days)
                    val isLockedOriginal = isLocked && isOriginal && isSelected
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (!isLockedOriginal) onDayToggle(day) },
                        label = { Text(dayLabels[day - 1]) },
                        enabled = !isLockedOriginal,
                        modifier = Modifier.defaultMinSize(minWidth = 42.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockSettingsSection(
    isPasswordLocked: Boolean,
    isTimeLocked: Boolean,
    timeLockUntilMillis: Long,
    onSetPassword: (String) -> Unit,
    onSetTimeLock: (Long) -> Unit,
    onExtendTimeLock: (Long) -> Unit,
    onVerifyAndClearPassword: (String) -> Boolean
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showTimeLockDialog by remember { mutableStateOf(false) }
    var showUnlockPasswordDialog by remember { mutableStateOf(false) }
    var showExtendTimeLockDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lock Settings",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isPasswordLocked) {
                // Password locked: show status + unlock button (requires password)
                Text(
                    text = "Password locked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showUnlockPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock with Password")
                }
            } else if (isTimeLocked) {
                // Time locked: show remaining time + extend button (NO clear/remove)
                val remainingMs = timeLockUntilMillis - System.currentTimeMillis()
                val remainingText = if (remainingMs > 0) formatDuration(remainingMs) else "Expiring..."
                Text(
                    text = "Time locked \u2014 $remainingText remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showExtendTimeLockDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Extend Lock")
                }
            } else {
                // Unlocked: show set lock options
                Text(
                    text = "Lock this profile to prevent modifications. Locks can only be extended, not removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Password Lock")
                    }
                    OutlinedButton(
                        onClick = { showTimeLockDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Time Lock")
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                onSetPassword(password)
                showPasswordDialog = false
            }
        )
    }

    if (showTimeLockDialog) {
        TimeLockDialog(
            title = "Lock Duration",
            onDismiss = { showTimeLockDialog = false },
            onConfirm = { durationMs ->
                onSetTimeLock(durationMs)
                showTimeLockDialog = false
            }
        )
    }

    if (showExtendTimeLockDialog) {
        TimeLockDialog(
            title = "Extend Lock By",
            onDismiss = { showExtendTimeLockDialog = false },
            onConfirm = { durationMs ->
                onExtendTimeLock(durationMs)
                showExtendTimeLockDialog = false
            }
        )
    }

    if (showUnlockPasswordDialog) {
        VerifyPasswordDialog(
            onDismiss = { showUnlockPasswordDialog = false },
            onVerify = { password ->
                val success = onVerifyAndClearPassword(password)
                if (success) {
                    showUnlockPasswordDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialMinute: Int,
    minMinute: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newMinute = state.hour * 60 + state.minute
                    // Enforce minimum if locked
                    val finalMinute = if (minMinute != null && newMinute < minMinute) {
                        minMinute
                    } else {
                        newMinute
                    }
                    onConfirm(finalMinute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty() && password == confirmPassword
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VerifyPasswordDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Password") },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onVerify(password) },
                enabled = password.isNotEmpty()
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeLockDialog(
    title: String = "Lock Duration",
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val options = listOf(
        "30 minutes" to 30 * 60 * 1000L,
        "1 hour" to 1 * 60 * 60 * 1000L,
        "2 hours" to 2 * 60 * 60 * 1000L,
        "4 hours" to 4 * 60 * 60 * 1000L,
        "8 hours" to 8 * 60 * 60 * 1000L,
        "24 hours" to 24 * 60 * 60 * 1000L,
        "1 week" to 7 * 24 * 60 * 60 * 1000L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, duration) ->
                    TextButton(
                        onClick = { onConfirm(duration) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

private fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return when {
        hours > 24 -> "${hours / 24}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
