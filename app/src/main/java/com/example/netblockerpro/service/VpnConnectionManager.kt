package com.example.netblockerpro.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages VPN connection lifecycle and state.
 * Provides methods to start/stop VPN and check prerequisites.
 */
@Singleton
class VpnConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val powerManager: PowerManager
) {
    
    /**
     * VPN connection states.
     */
    sealed class VpnState {
        data object Disconnected : VpnState()
        data object Connecting : VpnState()
        data object Connected : VpnState()
        data object Error : VpnState()
    }
    
    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()
    
    /**
     * Check if VPN permission has been granted.
     * Returns the prepare intent if permission is needed, null if already granted.
     */
    fun prepareVpn(): Intent? {
        return VpnService.prepare(context)
    }
    
    /**
     * Check if the app is battery-optimized (which would kill the VPN service).
     */
    fun isBatteryOptimized(): Boolean {
        return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    /**
     * Get intent to open battery optimization settings for this app.
     */
    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    /**
     * Start the VPN service.
     */
    fun startVpn() {
        _vpnState.value = VpnState.Connecting
        val intent = Intent(context, NetBlockerVpnService::class.java).apply {
            action = com.example.netblockerpro.util.Constants.ACTION_START_VPN
        }
        context.startForegroundService(intent)
    }
    
    /**
     * Stop the VPN service.
     */
    fun stopVpn() {
        val intent = Intent(context, NetBlockerVpnService::class.java).apply {
            action = com.example.netblockerpro.util.Constants.ACTION_STOP_VPN
        }
        context.startService(intent)
        _vpnState.value = VpnState.Disconnected
    }
    
    /**
     * Update VPN state (called from VPN service).
     */
    fun updateState(state: VpnState) {
        _vpnState.value = state
    }
    
    /**
     * Check if VPN is currently active.
     */
    val isVpnActive: Boolean
        get() = _vpnState.value == VpnState.Connected
}
