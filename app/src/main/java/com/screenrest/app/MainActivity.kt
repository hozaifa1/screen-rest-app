package com.screenrest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.screenrest.app.data.local.datastore.SettingsDataStore
import com.screenrest.app.presentation.navigation.NavGraph
import com.screenrest.app.presentation.theme.ScreenRestTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
