package com.screenrest.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.screenrest.app.domain.model.EnforcementLevel
import com.screenrest.app.domain.model.PermissionStatus

@Composable
fun CompleteStep(
    permissionStatus: PermissionStatus,
    enforcementLevel: EnforcementLevel,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "All Set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Here's your permission summary",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (enforcementLevel) {
                    EnforcementLevel.FULL -> MaterialTheme.colorScheme.primaryContainer
                    EnforcementLevel.STANDARD -> MaterialTheme.colorScheme.secondaryContainer
                    EnforcementLevel.BASIC -> MaterialTheme.colorScheme.tertiaryContainer
                    EnforcementLevel.NONE -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enforcement Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = enforcementLevel.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when (enforcementLevel) {
                        EnforcementLevel.FULL -> "Maximum break enforcement"
                        EnforcementLevel.STANDARD -> "Good break enforcement"
                        EnforcementLevel.BASIC -> "Basic break enforcement"
                        EnforcementLevel.NONE -> "Insufficient permissions"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PermissionCheckItem(
                name = "Usage Stats",
                isGranted = permissionStatus.usageStats
            )
            
            PermissionCheckItem(
                name = "Display Over Apps",
                isGranted = permissionStatus.overlay
            )
            
            PermissionCheckItem(
                name = "Notifications",
                isGranted = permissionStatus.notification
            )
            
            PermissionCheckItem(
                name = "Accessibility Service",
                isGranted = permissionStatus.accessibility
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = permissionStatus.usageStats && permissionStatus.overlay
        ) {
            Text(
                text = "Start Using ScreenRest",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        if (!permissionStatus.usageStats || !permissionStatus.overlay) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please grant required permissions to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PermissionCheckItem(
    name: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isGranted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGranted) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
