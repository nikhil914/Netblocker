package com.example.netblockerpro.service

import android.net.TrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks upload and download traffic statistics.
 * Provides speed calculations for the dashboard.
 */
@Singleton
class TrafficStatsTracker @Inject constructor() {
    
    data class TrafficData(
        val downloadSpeed: Long = 0, // bytes per second
        val uploadSpeed: Long = 0,   // bytes per second
        val totalDownload: Long = 0, // total bytes
        val totalUpload: Long = 0    // total bytes
    ) {
        val downloadSpeedFormatted: String
            get() = formatSpeed(downloadSpeed)
        
        val uploadSpeedFormatted: String
            get() = formatSpeed(uploadSpeed)
        
        private fun formatSpeed(bytesPerSecond: Long): String {
            return when {
                bytesPerSecond >= 1_000_000 -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000.0)
                bytesPerSecond >= 1_000 -> String.format("%.1f KB/s", bytesPerSecond / 1_000.0)
                else -> "$bytesPerSecond B/s"
            }
        }
    }
    
    private val _trafficData = MutableStateFlow(TrafficData())
    val trafficData: StateFlow<TrafficData> = _trafficData.asStateFlow()
    
    private var trackingJob: Job? = null
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L
    
    private val scope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Start tracking traffic statistics.
     */
    fun startTracking() {
        if (trackingJob?.isActive == true) return
        
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTimestamp = System.currentTimeMillis()
        
        trackingJob = scope.launch {
            while (isActive) {
                delay(1000) // Update every second
                
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()
                
                val timeDelta = (currentTime - lastTimestamp) / 1000.0
                if (timeDelta > 0) {
                    val downloadSpeed = ((currentRxBytes - lastRxBytes) / timeDelta).toLong()
                    val uploadSpeed = ((currentTxBytes - lastTxBytes) / timeDelta).toLong()
                    
                    _trafficData.value = TrafficData(
                        downloadSpeed = downloadSpeed.coerceAtLeast(0),
                        uploadSpeed = uploadSpeed.coerceAtLeast(0),
                        totalDownload = currentRxBytes,
                        totalUpload = currentTxBytes
                    )
                }
                
                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTimestamp = currentTime
            }
        }
    }
    
    /**
     * Stop tracking traffic statistics.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _trafficData.value = TrafficData()
    }
}
