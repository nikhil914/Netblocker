package com.example.netblockerpro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.netblockerpro.service.NetworkMonitor
import com.example.netblockerpro.service.TrafficStatsTracker
import com.example.netblockerpro.service.VpnConnectionManager
import com.example.netblockerpro.ui.theme.AllowedColor
import com.example.netblockerpro.ui.theme.BlockedColor
import com.example.netblockerpro.ui.theme.CellularColor
import com.example.netblockerpro.ui.theme.WifiColor

@Composable
fun DashboardCard(
    vpnState: VpnConnectionManager.VpnState,
    networkType: NetworkMonitor.NetworkType,
    trafficData: TrafficStatsTracker.TrafficData,
    blockedAppsCount: Int,
    totalBlockedConnections: Int,
    onToggleVpn: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = vpnState == VpnConnectionManager.VpnState.Connected
    val isConnecting = vpnState == VpnConnectionManager.VpnState.Connecting
    
    val backgroundGradient = if (isActive) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundGradient)
                .padding(20.dp)
        ) {
            Column {
                // Top row: Status + Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Shield icon
                        val iconColor by animateColorAsState(
                            targetValue = if (isActive) AllowedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "iconColor"
                        )
                        
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = iconColor.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                text = when (vpnState) {
                                    VpnConnectionManager.VpnState.Connected -> "Firewall Active"
                                    VpnConnectionManager.VpnState.Connecting -> "Connecting..."
                                    VpnConnectionManager.VpnState.Error -> "Error"
                                    VpnConnectionManager.VpnState.Disconnected -> "Firewall Inactive"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val (networkIcon, networkColor, networkLabel) = when (networkType) {
                                    NetworkMonitor.NetworkType.Wifi -> Triple(Icons.Default.Wifi, WifiColor, "WiFi")
                                    NetworkMonitor.NetworkType.Cellular -> Triple(Icons.Default.SignalCellular4Bar, CellularColor, "Mobile Data")
                                    NetworkMonitor.NetworkType.None -> Triple(Icons.Default.Block, MaterialTheme.colorScheme.onSurfaceVariant, "No Connection")
                                }
                                
                                Icon(
                                    imageVector = networkIcon,
                                    contentDescription = null,
                                    tint = networkColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = networkLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Master switch
                    val switchScale by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.95f,
                        label = "switchScale"
                    )
                    
                    Switch(
                        checked = isActive || isConnecting,
                        onCheckedChange = onToggleVpn,
                        modifier = Modifier.scale(switchScale),
                        enabled = !isConnecting,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AllowedColor,
                            checkedTrackColor = AllowedColor.copy(alpha = 0.4f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Download,
                        label = "Download",
                        value = trafficData.downloadSpeedFormatted,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    StatItem(
                        icon = Icons.Default.Upload,
                        label = "Upload",
                        value = trafficData.uploadSpeedFormatted,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    StatItem(
                        icon = Icons.Default.Block,
                        label = "Blocked",
                        value = totalBlockedConnections.toString(),
                        color = BlockedColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
