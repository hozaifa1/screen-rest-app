package com.screenrest.app.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.screenrest.app.data.local.datastore.SettingsDataStore

@Composable
fun NavGraph(
    navController: NavHostController,
    settingsDataStore: SettingsDataStore
) {
    val onboardingCompleted by settingsDataStore.onboardingCompleted.collectAsState(initial = false)
    
    val startDestination = if (onboardingCompleted) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            com.screenrest.app.presentation.onboarding.OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            com.screenrest.app.presentation.main.HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            com.screenrest.app.presentation.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCustomMessages = {
                    navController.navigate(Screen.CustomMessages.route)
                }
            )
        }
        
        composable(Screen.BreakConfig.route) {
            // Break config screen placeholder - will be implemented in Part 3
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Break Config Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.CustomMessages.route) {
            com.screenrest.app.presentation.settings.messages.CustomMessagesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Permissions.route) {
            // Permissions screen placeholder - will be implemented in Part 3
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Permissions Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.About.route) {
            // About screen placeholder - will be implemented in Part 3
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("About Screen - Coming in Part 3")
            }
        }
    }
}
