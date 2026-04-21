package com.screenrest.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.screenrest.app.data.local.datastore.SettingsDataStore
import com.screenrest.app.presentation.navigation.NavGraph
import com.screenrest.app.presentation.theme.ScreenRestTheme
import com.screenrest.app.service.ServiceController
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start BlockTimeSchedulerService on every app launch if onboarding is complete
        startSchedulerIfReady()
        
        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(
                initial = com.screenrest.app.domain.model.ThemeMode.SYSTEM
            )
            val themeColor by settingsDataStore.themeColor.collectAsState(
                initial = com.screenrest.app.domain.model.ThemeColor.TEAL
            )
            val navController = rememberNavController()
            
            ScreenRestTheme(themeMode = themeMode, themeColor = themeColor) {
                NavGraph(
                    navController = navController,
                    settingsDataStore = settingsDataStore
                )
            }
        }
    }
    
    private fun startSchedulerIfReady() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val onboardingComplete = settingsDataStore.onboardingCompleted.first()
                if (onboardingComplete) {
                    ServiceController.startBlockTimeScheduler(this@MainActivity)
                    Log.d("MainActivity", "BlockTimeSchedulerService started")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting scheduler", e)
            }
        }
    }
}
