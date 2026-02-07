package com.example.netblockerpro.service

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity and provides O(1) lookup for current network type.
 * Uses NetworkCallback for event-driven updates instead of polling.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {
    
    /**
     * Represents the current network type.
     */
    sealed class NetworkType {
        data object Wifi : NetworkType()
        data object Cellular : NetworkType()
        data object None : NetworkType()
        
        override fun toString(): String = when (this) {
            is Wifi -> "WIFI"
            is Cellular -> "CELLULAR"
            is None -> "NONE"
        }
    }
    
    // Atomic reference for zero-cost reads in VPN loop
    private val _currentNetworkAtomic = AtomicReference<NetworkType>(NetworkType.None)
    
    // StateFlow for UI observation
    private val _currentNetwork = MutableStateFlow<NetworkType>(NetworkType.None)
    val currentNetwork: StateFlow<NetworkType> = _currentNetwork.asStateFlow()
    
    /**
     * Get current network type with O(1) performance.
     * Safe to call from packet processing loop.
     */
    val networkType: NetworkType
        get() = _currentNetworkAtomic.get()
    
    private var isMonitoring = false
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        
        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            val type = determineNetworkType(capabilities)
            updateNetworkType(type)
        }
        
        override fun onLost(network: Network) {
            // Check if there's still an active network
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                updateNetworkType(NetworkType.None)
            } else {
                // Get capabilities of the remaining network
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                val type = caps?.let { determineNetworkType(it) } ?: NetworkType.None
                updateNetworkType(type)
            }
        }
        
        override fun onAvailable(network: Network) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            val type = caps?.let { determineNetworkType(it) } ?: NetworkType.None
            updateNetworkType(type)
        }
    }
    
    private fun determineNetworkType(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Wifi // Treat ethernet as WiFi
            else -> NetworkType.None
        }
    }
    
    private fun updateNetworkType(type: NetworkType) {
        _currentNetworkAtomic.set(type)
        _currentNetwork.value = type
    }
    
    /**
     * Start monitoring network changes.
     * Call from VPN service onCreate.
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isMonitoring = true
            
            // Initialize with current state
            val activeNetwork = connectivityManager.activeNetwork
            val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val type = caps?.let { determineNetworkType(it) } ?: NetworkType.None
            updateNetworkType(type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Stop monitoring network changes.
     * Call from VPN service onDestroy.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isMonitoring = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
