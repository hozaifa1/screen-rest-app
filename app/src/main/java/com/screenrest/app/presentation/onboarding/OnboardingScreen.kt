package com.screenrest.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 6f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState.currentStep) {
                    0 -> WelcomeStep(
                        onNext = { viewModel.nextStep() }
                    )
                    1 -> UsageAccessStep(
                        isGranted = uiState.permissionStatus.usageStats,
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onRefresh = { viewModel.refreshPermissions() }
                    )
                    2 -> OverlayStep(
                        isGranted = uiState.permissionStatus.overlay,
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onRefresh = { viewModel.refreshPermissions() }
                    )
                    3 -> NotificationStep(
                        isGranted = uiState.permissionStatus.notification,
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onRefresh = { viewModel.refreshPermissions() }
                    )
                    4 -> AccessibilityStep(
                        isGranted = uiState.permissionStatus.accessibility,
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onRefresh = { viewModel.refreshPermissions() }
                    )
                    5 -> CompleteStep(
                        permissionStatus = uiState.permissionStatus,
                        enforcementLevel = uiState.enforcementLevel,
                        onComplete = {
                            viewModel.completeOnboarding()
                            onComplete()
                        },
                        onBack = { viewModel.previousStep() }
                    )
                }
            }
        }
    }
}
