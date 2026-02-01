package com.screenrest.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenrest.app.data.local.datastore.SettingsDataStore
import com.screenrest.app.service.ServiceController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val onboardingComplete = settingsDataStore.onboardingCompleted.first()
                val trackingEnabled = settingsDataStore.usageTrackingEnabled.first()
                
                if (onboardingComplete && trackingEnabled) {
                    ServiceController.startTracking(context)
                }
            }
        }
    }
}
