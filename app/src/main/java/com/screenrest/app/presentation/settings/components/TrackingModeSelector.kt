package com.screenrest.app.presentation.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.screenrest.app.domain.model.TrackingMode

@Composable
fun TrackingModeSelector(
    selectedMode: TrackingMode,
    onModeSelected: (TrackingMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tracking Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        TrackingMode.entries.forEach { mode ->
            TrackingModeItem(
                mode = mode,
                isSelected = selectedMode == mode,
                onSelect = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun TrackingModeItem(
    mode: TrackingMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (mode) {
                            TrackingMode.CONTINUOUS -> "Continuous"
                            TrackingMode.CUMULATIVE_DAILY -> "Cumulative Daily"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = when (mode) {
                            TrackingMode.CONTINUOUS -> "Resets after each break"
                            TrackingMode.CUMULATIVE_DAILY -> "Accumulates throughout the day"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = when (mode) {
                            TrackingMode.CONTINUOUS -> "Tracks continuous screen usage. After reaching the threshold, a break is triggered. The timer resets after each break, allowing fresh usage tracking."
                            TrackingMode.CUMULATIVE_DAILY -> "Tracks total screen time throughout the day. Usage accumulates across sessions. When you reach the threshold, a break is triggered and the timer continues accumulating. Resets at midnight."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
