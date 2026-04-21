package com.screenrest.app.presentation.components

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.screenrest.app.util.DeviceAdminHelper
import com.screenrest.app.util.openAccessibilitySettings
import com.screenrest.app.util.openNotificationSettings
import com.screenrest.app.util.openOverlaySettings
import com.screenrest.app.util.openUsageAccessSettings

@Composable
fun PermissionWarningCard(
    title: String,
    description: String,
    permissionType: String
) {
    val context = LocalContext.current
    
    // Critical permissions use error styling, optional ones use softer styling
    val isCritical = permissionType == "usageStats" || permissionType == "overlay"
    val containerColor = if (isCritical) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val iconTint = if (isCritical) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (isCritical) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleColor = if (isCritical) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isCritical) Icons.Default.Warning else Icons.Outlined.Info,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val buttonColor = if (isCritical) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
                
                Button(
                    onClick = {
                        when (permissionType) {
                            "usageStats" -> context.openUsageAccessSettings()
                            "overlay" -> context.openOverlaySettings()
                            "accessibility" -> context.openAccessibilitySettings()
                            "notification" -> context.openNotificationSettings()
                            "deviceAdmin" -> {
                                (context as? Activity)?.let { activity ->
                                    DeviceAdminHelper.requestActivation(activity)
                                }
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                    colors = buttonColor
                ) {
                    Text(
                        if (isCritical) "Grant Permission" else "Enable",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
