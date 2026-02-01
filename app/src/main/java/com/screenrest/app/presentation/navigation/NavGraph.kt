package com.screenrest.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            // Onboarding screen placeholder - will be implemented in Part 3
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Onboarding Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.Home.route) {
            // Home screen placeholder - will be implemented in Part 3
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Home Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.Settings.route) {
            // Settings screen placeholder - will be implemented in Part 3
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Settings Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.BreakConfig.route) {
            // Break config screen placeholder - will be implemented in Part 3
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Break Config Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.CustomMessages.route) {
            // Custom messages screen placeholder - will be implemented in Part 3
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Custom Messages Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.Permissions.route) {
            // Permissions screen placeholder - will be implemented in Part 3
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("Permissions Screen - Coming in Part 3")
            }
        }
        
        composable(Screen.About.route) {
            // About screen placeholder - will be implemented in Part 3
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("About Screen - Coming in Part 3")
            }
        }
    }
}
