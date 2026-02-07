package com.example.netblockerpro.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.netblockerpro.service.NetBlockerVpnService
import com.example.netblockerpro.service.VpnConnectionManager
import com.example.netblockerpro.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile for toggling the firewall.
 */
@AndroidEntryPoint
class FirewallTileService : TileService() {
    
    @Inject
    lateinit var vpnConnectionManager: VpnConnectionManager
    
    private val tileScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onStartListening() {
        super.onStartListening()
        
        tileScope.launch {
            vpnConnectionManager.vpnState.collectLatest { state ->
                updateTile(state)
            }
        }
    }
    
    override fun onStopListening() {
        super.onStopListening()
        tileScope.cancel()
    }
    
    override fun onClick() {
        super.onClick()
        
        when (vpnConnectionManager.vpnState.value) {
            VpnConnectionManager.VpnState.Connected,
            VpnConnectionManager.VpnState.Connecting -> {
                // Stop VPN
                vpnConnectionManager.stopVpn()
            }
            VpnConnectionManager.VpnState.Disconnected,
            VpnConnectionManager.VpnState.Error -> {
                // Check if VPN permission is granted
                val prepareIntent = vpnConnectionManager.prepareVpn()
                if (prepareIntent != null) {
                    // Need to open app for permission
                    unlockAndRun {
                        startActivityAndCollapse(prepareIntent.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                } else {
                    // Start VPN directly
                    vpnConnectionManager.startVpn()
                }
            }
        }
    }
    
    private fun updateTile(state: VpnConnectionManager.VpnState) {
        qsTile?.let { tile ->
            when (state) {
                VpnConnectionManager.VpnState.Connected -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "Firewall On"
                    tile.subtitle = "Protected"
                }
                VpnConnectionManager.VpnState.Connecting -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "Connecting..."
                    tile.subtitle = null
                }
                VpnConnectionManager.VpnState.Disconnected -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "Firewall Off"
                    tile.subtitle = "Tap to enable"
                }
                VpnConnectionManager.VpnState.Error -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "Firewall Error"
                    tile.subtitle = "Tap to retry"
                }
            }
            tile.updateTile()
        }
    }
    
    private fun unlockAndRun(action: () -> Unit) {
        if (isLocked) {
            unlockAndRun(action)
        } else {
            action()
        }
    }
}
