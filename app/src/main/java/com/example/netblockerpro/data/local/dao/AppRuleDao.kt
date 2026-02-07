package com.example.netblockerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.netblockerpro.data.local.entity.AppRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for app firewall rules.
 * Supports Flow-based reactive queries for UI updates.
 */
@Dao
interface AppRuleDao {
    
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppRuleEntity>>
    
    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getRule(packageName: String): AppRuleEntity?
    
    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    fun getRuleFlow(packageName: String): Flow<AppRuleEntity?>
    
    @Upsert
    suspend fun upsertRule(rule: AppRuleEntity)
    
    @Upsert
    suspend fun upsertRules(rules: List<AppRuleEntity>)
    
    @Query("UPDATE app_rules SET isWifiBlocked = :blocked, lastUpdated = :timestamp")
    suspend fun setAllWifiBlocked(blocked: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_rules SET isDataBlocked = :blocked, lastUpdated = :timestamp")
    suspend fun setAllDataBlocked(blocked: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_rules SET isWifiBlocked = :wifiBlocked, isDataBlocked = :dataBlocked, lastUpdated = :timestamp WHERE packageName = :packageName")
    suspend fun updateRule(
        packageName: String, 
        wifiBlocked: Boolean, 
        dataBlocked: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT COUNT(*) FROM app_rules WHERE isWifiBlocked = 1 OR isDataBlocked = 1")
    fun getBlockedAppsCount(): Flow<Int>
    
    @Query("SELECT * FROM app_rules WHERE isWifiBlocked = 1 OR isDataBlocked = 1")
    suspend fun getBlockedRules(): List<AppRuleEntity>
}
