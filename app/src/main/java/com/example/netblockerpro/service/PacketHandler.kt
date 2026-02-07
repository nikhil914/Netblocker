package com.example.netblockerpro.service

import android.util.LruCache
import com.example.netblockerpro.data.local.dao.AppRuleDao
import com.example.netblockerpro.data.local.dao.ConnectionLogDao
import com.example.netblockerpro.data.local.entity.AppRuleEntity
import com.example.netblockerpro.data.local.entity.ConnectionLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles packet inspection and filtering logic.
 * Uses cached rules for O(1) lookups in the packet processing loop.
 */
@Singleton
class PacketHandler @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val appRuleDao: AppRuleDao,
    private val connectionLogDao: ConnectionLogDao
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // LRU cache for UID -> Rule mapping (capacity 150 apps)
    private val ruleCache = LruCache<Int, AppRuleEntity?>(150)
    
    // Batch logging buffer
    private val logBuffer = ConcurrentLinkedQueue<ConnectionLogEntity>()
    private var lastFlushTime = System.currentTimeMillis()
    private val flushIntervalMs = 500L
    private val maxBufferSize = 50
    
    // Package manager cache for UID -> PackageName
    private val uidToPackageCache = LruCache<Int, String>(150)
    
    /**
     * Result of packet analysis.
     */
    data class PacketDecision(
        val shouldBlock: Boolean,
        val sourceUid: Int,
        val destIp: String,
        val destPort: Int,
        val networkType: NetworkMonitor.NetworkType
    )
    
    /**
     * Analyze a packet and determine if it should be blocked.
     * This is the hot path - must be extremely fast.
     */
    fun analyzePacket(
        buffer: ByteBuffer,
        uidLookup: (destIp: String, destPort: Int, protocol: Int) -> Int
    ): PacketDecision? {
        val packet = buffer.duplicate()
        
        // Parse IP header
        val ipVersion = (packet.get(0).toInt() and 0xF0) shr 4
        
        return when (ipVersion) {
            4 -> analyzeIpv4Packet(packet, uidLookup)
            6 -> analyzeIpv6Packet(packet, uidLookup)
            else -> null
        }
    }
    
    private fun analyzeIpv4Packet(
        packet: ByteBuffer,
        uidLookup: (destIp: String, destPort: Int, protocol: Int) -> Int
    ): PacketDecision? {
        if (packet.remaining() < 20) return null
        
        val headerLength = (packet.get(0).toInt() and 0x0F) * 4
        val protocol = packet.get(9).toInt() and 0xFF
        
        // Extract destination IP
        val destIpBytes = ByteArray(4)
        packet.position(16)
        packet.get(destIpBytes)
        val destIp = destIpBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
        
        // Extract destination port (for TCP/UDP)
        var destPort = 0
        if ((protocol == 6 || protocol == 17) && packet.remaining() >= headerLength + 4) {
            packet.position(headerLength + 2)
            destPort = packet.short.toInt() and 0xFFFF
        }
        
        // Get UID for this connection
        val uid = uidLookup(destIp, destPort, protocol)
        if (uid <= 0) return null
        
        // Get network type (O(1) atomic read)
        val networkType = networkMonitor.networkType
        
        // Check if should block
        val shouldBlock = shouldBlockPacket(uid, networkType)
        
        return PacketDecision(
            shouldBlock = shouldBlock,
            sourceUid = uid,
            destIp = destIp,
            destPort = destPort,
            networkType = networkType
        )
    }
    
    private fun analyzeIpv6Packet(
        packet: ByteBuffer,
        uidLookup: (destIp: String, destPort: Int, protocol: Int) -> Int
    ): PacketDecision? {
        if (packet.remaining() < 40) return null
        
        val nextHeader = packet.get(6).toInt() and 0xFF
        
        // Extract destination IP
        val destIpBytes = ByteArray(16)
        packet.position(24)
        packet.get(destIpBytes)
        val destIp = formatIpv6(destIpBytes)
        
        // Extract destination port (for TCP/UDP)
        var destPort = 0
        if ((nextHeader == 6 || nextHeader == 17) && packet.remaining() >= 44) {
            packet.position(42)
            destPort = packet.short.toInt() and 0xFFFF
        }
        
        // Get UID for this connection
        val uid = uidLookup(destIp, destPort, nextHeader)
        if (uid <= 0) return null
        
        // Get network type (O(1) atomic read)
        val networkType = networkMonitor.networkType
        
        // Check if should block
        val shouldBlock = shouldBlockPacket(uid, networkType)
        
        return PacketDecision(
            shouldBlock = shouldBlock,
            sourceUid = uid,
            destIp = destIp,
            destPort = destPort,
            networkType = networkType
        )
    }
    
    private fun formatIpv6(bytes: ByteArray): String {
        return buildString {
            for (i in bytes.indices step 2) {
                if (i > 0) append(":")
                append(String.format("%02x%02x", bytes[i], bytes[i + 1]))
            }
        }
    }
    
    /**
     * Check if a packet from this UID should be blocked on current network.
     * Uses cached rules for O(1) lookup.
     */
    private fun shouldBlockPacket(uid: Int, networkType: NetworkMonitor.NetworkType): Boolean {
        val rule = ruleCache.get(uid)
        
        if (rule == null) {
            // Cache miss - load asynchronously, don't block for now
            scope.launch {
                loadRuleForUid(uid)
            }
            return false
        }
        
        return when (networkType) {
            NetworkMonitor.NetworkType.Wifi -> rule.isWifiBlocked
            NetworkMonitor.NetworkType.Cellular -> rule.isDataBlocked
            NetworkMonitor.NetworkType.None -> false
        }
    }
    
    /**
     * Load rule for UID from database and cache it.
     */
    private suspend fun loadRuleForUid(uid: Int) {
        val packageName = uidToPackageCache.get(uid) ?: return
        val rule = appRuleDao.getRule(packageName)
        if (rule != null) {
            ruleCache.put(uid, rule)
        }
    }
    
    /**
     * Register a UID -> PackageName mapping.
     * Called when VPN service resolves UIDs.
     */
    fun registerUidPackage(uid: Int, packageName: String) {
        uidToPackageCache.put(uid, packageName)
    }
    
    /**
     * Preload rules for all blocked apps.
     * Called on VPN service start.
     */
    suspend fun preloadRules(uidToPackageMap: Map<Int, String>) {
        uidToPackageMap.forEach { (uid, packageName) ->
            uidToPackageCache.put(uid, packageName)
        }
        
        val rules = appRuleDao.getBlockedRules()
        rules.forEach { rule ->
            uidToPackageMap.entries.find { it.value == rule.packageName }?.let { (uid, _) ->
                ruleCache.put(uid, rule)
            }
        }
    }
    
    /**
     * Invalidate cached rule for a package.
     * Called when user changes app rules.
     */
    fun invalidateRule(uid: Int) {
        ruleCache.remove(uid)
    }
    
    /**
     * Clear all cached rules.
     */
    fun clearCache() {
        ruleCache.evictAll()
        uidToPackageCache.evictAll()
    }
    
    /**
     * Log a blocked connection (batched).
     */
    fun logBlockedConnection(
        packageName: String,
        appName: String,
        destIp: String,
        destPort: Int,
        networkType: String
    ) {
        val log = ConnectionLogEntity(
            packageName = packageName,
            appName = appName,
            destinationIp = destIp,
            destinationPort = destPort,
            networkType = networkType,
            wasBlocked = true
        )
        logBuffer.offer(log)
        
        // Flush if buffer is full or time threshold exceeded
        val now = System.currentTimeMillis()
        if (logBuffer.size >= maxBufferSize || (now - lastFlushTime) >= flushIntervalMs) {
            flushLogs()
        }
    }
    
    /**
     * Flush buffered logs to database.
     */
    private fun flushLogs() {
        if (logBuffer.isEmpty()) return
        
        val logsToFlush = mutableListOf<ConnectionLogEntity>()
        while (logBuffer.isNotEmpty()) {
            logBuffer.poll()?.let { logsToFlush.add(it) }
        }
        
        if (logsToFlush.isNotEmpty()) {
            scope.launch {
                connectionLogDao.insertLogs(logsToFlush)
            }
        }
        
        lastFlushTime = System.currentTimeMillis()
    }
    
    /**
     * Force flush remaining logs.
     * Called on VPN service stop.
     */
    fun forceFlush() {
        flushLogs()
    }
}
