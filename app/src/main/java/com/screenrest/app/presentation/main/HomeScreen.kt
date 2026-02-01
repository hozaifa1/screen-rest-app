package com.screenrest.app.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenrest.app.presentation.main.components.ConfigSummaryCard
import com.screenrest.app.presentation.main.components.StatusCard
import com.screenrest.app.presentation.components.PermissionWarningCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScreenRest") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                isServiceRunning = uiState.isServiceRunning,
                enforcementLevel = uiState.enforcementLevel,
                onToggleService = { viewModel.toggleService() }
            )
            
            ConfigSummaryCard(
                breakConfig = uiState.breakConfig,
                onEditClick = onNavigateToSettings
            )
            
            if (!uiState.permissionStatus.usageStats) {
                PermissionWarningCard(
                    title = "Usage Access Required",
                    description = "Cannot track screen time without this permission",
                    permissionType = "usageStats"
                )
            }
            
            if (!uiState.permissionStatus.overlay) {
                PermissionWarningCard(
                    title = "Overlay Permission Required",
                    description = "Breaks will only show as notifications without this permission",
                    permissionType = "overlay"
                )
            }
            
            if (!uiState.permissionStatus.accessibility && uiState.permissionStatus.usageStats && uiState.permissionStatus.overlay) {
                PermissionWarningCard(
                    title = "Accessibility Service Optional",
                    description = "Break screen can be bypassed with home button. Enable for stricter enforcement.",
                    permissionType = "accessibility"
                )
            }
        }
    }
}
