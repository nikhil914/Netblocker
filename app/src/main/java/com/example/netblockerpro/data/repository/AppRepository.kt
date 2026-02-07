package com.example.netblockerpro.data.repository

import com.example.netblockerpro.data.local.entity.AppRuleEntity
import com.example.netblockerpro.data.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing app rules and installed apps.
 */
interface AppRepository {
    
    /**
     * Get all installed apps with their current rules.
     * Combines PackageManager data with Room rules.
     */
    fun getInstalledApps(): Flow<List<AppInfo>>
    
    /**
     * Get all app rules from database.
     */
    fun getAllRules(): Flow<List<AppRuleEntity>>
    
    /**
     * Update a single app's firewall rules.
     */
    suspend fun updateAppRule(packageName: String, wifiBlocked: Boolean, dataBlocked: Boolean)
    
    /**
     * Set WiFi blocked for all apps.
     */
    suspend fun blockAllWifi(blocked: Boolean)
    
    /**
     * Set Mobile Data blocked for all apps.
     */
    suspend fun blockAllData(blocked: Boolean)
    
    /**
     * Get rule for a specific app.
     */
    suspend fun getRule(packageName: String): AppRuleEntity?
    
    /**
     * Sync installed apps with database.
     * Called on app startup to handle newly installed/uninstalled apps.
     */
    suspend fun syncInstalledApps()
    
    /**
     * Get count of blocked apps.
     */
    fun getBlockedAppsCount(): Flow<Int>
}
