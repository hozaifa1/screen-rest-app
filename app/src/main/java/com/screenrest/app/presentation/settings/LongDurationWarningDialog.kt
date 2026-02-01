package com.screenrest.app.presentation.settings

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun LongDurationWarningDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("⚠️", style = MaterialTheme.typography.headlineMedium)
        },
        title = {
            Text("Long Break Duration")
        },
        text = {
            Text("You've set a break duration longer than 2 minutes. While longer breaks can be beneficial, make sure this aligns with your goals.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
