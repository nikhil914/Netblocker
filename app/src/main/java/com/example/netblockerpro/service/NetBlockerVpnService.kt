package com.example.netblockerpro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.netblockerpro.R
import com.example.netblockerpro.data.local.dao.AppRuleDao
import com.example.netblockerpro.ui.MainActivity
import com.example.netblockerpro.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream
import javax.inject.Inject

/**
 * Core VPN service implementing the local firewall.
 * Intercepts all network traffic and applies per-app blocking rules.
 */
@AndroidEntryPoint
class NetBlockerVpnService : VpnService() {
    
    @Inject
    lateinit var networkMonitor: NetworkMonitor
    
    @Inject
    lateinit var packetHandler: PacketHandler
    
    @Inject
    lateinit var trafficStatsTracker: TrafficStatsTracker
    
    @Inject
    lateinit var vpnConnectionManager: VpnConnectionManager
    
    @Inject
    lateinit var appRuleDao: AppRuleDao
    
    @Inject
    lateinit var appPackageManager: PackageManager
    
    @Inject
    lateinit var notificationManager: NotificationManager
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @Volatile
    private var isRunning = false
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START_VPN -> startVpn()
            Constants.ACTION_STOP_VPN -> stopVpn()
        }
        return START_STICKY
    }
    
    private fun startVpn() {
        if (isRunning) return
        
        // Start foreground service
        startForeground(Constants.VPN_NOTIFICATION_ID, createNotification())
        
        // Start network monitoring
        networkMonitor.startMonitoring()
        trafficStatsTracker.startTracking()
        
        // Build VPN interface and start packet processing in coroutine
        serviceScope.launch {
            try {
                vpnInterface = createVpnInterface()
                isRunning = true
                vpnConnectionManager.updateState(VpnConnectionManager.VpnState.Connected)
                
                // Preload rules
                val uidMap = buildUidPackageMap()
                packetHandler.preloadRules(uidMap)
                
                // Start simple packet sink (just discard packets for blocked apps)
                processPackets()
            } catch (e: Exception) {
                e.printStackTrace()
                vpnConnectionManager.updateState(VpnConnectionManager.VpnState.Error)
                stopSelf()
            }
        }
    }
    
    private fun stopVpn() {
        isRunning = false
        
        // Flush remaining logs
        packetHandler.forceFlush()
        packetHandler.clearCache()
        
        // Stop monitoring
        networkMonitor.stopMonitoring()
        trafficStatsTracker.stopTracking()
        
        // Close VPN interface
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface = null
        
        // Update state
        vpnConnectionManager.updateState(VpnConnectionManager.VpnState.Disconnected)
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private suspend fun createVpnInterface(): ParcelFileDescriptor {
        val builder = Builder()
            .setSession("NetBlocker Pro")
            .setMtu(Constants.VPN_MTU)
            .addAddress(Constants.VPN_ADDRESS_V4, 32)
            .addAddress(Constants.VPN_ADDRESS_V6, 128)
            // Route only blocked apps through VPN - they will have no internet
            // Don't add routes for allowed apps - they bypass VPN entirely
            .setBlocking(false)
        
        // Always allow this app to bypass the VPN
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Get current network type
        val networkType = networkMonitor.networkType
        
        // Get blocked apps and add them to VPN (their traffic will be routed to VPN and dropped)
        val blockedPackages = getBlockedPackages(networkType)
        
        // Use disallowed approach: allow all apps EXCEPT blocked ones
        // But we need to do the opposite - block specific apps by routing through VPN
        // Actually, we'll use the "allowed" approach: only specific apps go through VPN
        
        if (blockedPackages.isEmpty()) {
            // No apps to block - set up a minimal VPN that doesn't affect traffic
            // Add a route that doesn't match anything real to keep VPN alive but harmless
            builder.addRoute("10.255.255.255", 32)
        } else {
            // Route all traffic through VPN by default
            builder.addRoute(Constants.VPN_ROUTE_V4, 0)
            builder.addRoute(Constants.VPN_ROUTE_V6, 0)
            
            // Allow all apps to bypass EXCEPT the blocked ones
            val pm = appPackageManager
            val installedApps = pm.getInstalledApplications(0)
            
            for (appInfo in installedApps) {
                // Skip our own app
                if (appInfo.packageName == packageName) continue
                
                // If app is NOT in blocked list, allow it to bypass VPN
                if (appInfo.packageName !in blockedPackages) {
                    try {
                        builder.addDisallowedApplication(appInfo.packageName)
                    } catch (e: Exception) {
                        // Package might not be valid or installed
                    }
                }
            }
        }
        
        // Add DNS servers for the VPN interface
        builder.addDnsServer("8.8.8.8")
        builder.addDnsServer("8.8.4.4")
        
        return builder.establish()
            ?: throw IllegalStateException("Failed to establish VPN interface")
    }
    
    /**
     * Get list of package names that should be blocked on current network.
     */
    private suspend fun getBlockedPackages(networkType: NetworkMonitor.NetworkType): Set<String> {
        val rules = appRuleDao.getBlockedRules()
        return rules.filter { rule ->
            when (networkType) {
                NetworkMonitor.NetworkType.Wifi -> rule.isWifiBlocked
                NetworkMonitor.NetworkType.Cellular -> rule.isDataBlocked
                NetworkMonitor.NetworkType.None -> false
            }
        }.map { it.packageName }.toSet()
    }
    
    /**
     * Simple packet sink - just read and discard packets.
     * Since we use per-app VPN routing, any packets that arrive here are from blocked apps.
     * We just read them to prevent buffer overflow and discard them.
     */
    private suspend fun processPackets() {
        val vpn = vpnInterface ?: return
        val inputStream = FileInputStream(vpn.fileDescriptor)
        
        val buffer = ByteArray(Constants.VPN_MTU)
        
        while (isRunning) {
            try {
                // Just read packets to drain the VPN interface buffer
                // They will be discarded, effectively blocking the app's traffic
                val length = inputStream.read(buffer)
                
                if (length <= 0) {
                    // Small delay to prevent busy loop when no data
                    delay(10)
                }
                // Packet is discarded - traffic blocked
            } catch (e: Exception) {
                if (isRunning) {
                    // Log error but continue running
                    e.printStackTrace()
                }
            }
        }
        
        try {
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun buildUidPackageMap(): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        try {
            val apps = appPackageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            apps.forEach { appInfo ->
                map[appInfo.uid] = appInfo.packageName
                packetHandler.registerUidPackage(appInfo.uid, appInfo.packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.VPN_NOTIFICATION_CHANNEL_ID,
            Constants.VPN_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the firewall is active"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, NetBlockerVpnService::class.java).apply {
                action = Constants.ACTION_STOP_VPN
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, Constants.VPN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("NetBlocker Pro Active")
            .setContentText("Firewall is protecting your device")
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_vpn_key, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
    
    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }
}
