package com.example.netblockerpro.ui.screen.home.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.netblockerpro.data.model.AppInfo
import com.example.netblockerpro.ui.theme.BlockedColor
import com.example.netblockerpro.ui.theme.CellularColor
import com.example.netblockerpro.ui.theme.WifiColor
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun AppListItem(
    app: AppInfo,
    onWifiToggle: () -> Unit,
    onDataToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        painter = rememberDrawablePainter(drawable = app.icon),
                        contentDescription = app.appName,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = app.appName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // App Name and Package
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (app.isSystemApp) "System App" else "User App",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Toggle buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WiFi toggle
                NetworkToggleButton(
                    isBlocked = app.isWifiBlocked,
                    onClick = onWifiToggle,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "WiFi",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    activeColor = WifiColor,
                    blockedColor = BlockedColor
                )
                
                // Data toggle
                NetworkToggleButton(
                    isBlocked = app.isDataBlocked,
                    onClick = onDataToggle,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SignalCellular4Bar,
                            contentDescription = "Mobile Data",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    activeColor = CellularColor,
                    blockedColor = BlockedColor
                )
            }
        }
    }
}

@Composable
private fun NetworkToggleButton(
    isBlocked: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    activeColor: Color,
    blockedColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isBlocked) {
            blockedColor.copy(alpha = 0.15f)
        } else {
            activeColor.copy(alpha = 0.15f)
        },
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides if (isBlocked) blockedColor else activeColor
                ) {
                    icon()
                }
            }
        }
    }
}
