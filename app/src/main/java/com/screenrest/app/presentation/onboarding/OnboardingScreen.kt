package com.screenrest.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Poll permission state while on any permission step so that granting a
    // permission in the system Settings app is auto-detected and the user is
    // moved forward without having to tap a manual "I've granted it" button.
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep in 1..5) {
            while (true) {
                viewModel.refreshPermissions()
                delay(400L)
            }
        }
    }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 7f },
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
                    5 -> DeviceAdminStep(
                        isGranted = uiState.permissionStatus.deviceAdmin,
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onRefresh = { viewModel.refreshPermissions() }
                    )
                    6 -> CompleteStep(
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
