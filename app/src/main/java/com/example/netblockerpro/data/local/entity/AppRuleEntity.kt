package com.example.netblockerpro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an app's firewall rules.
 * Stores per-app blocking configuration for WiFi and Mobile Data.
 */
@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isWifiBlocked: Boolean = false,
    val isDataBlocked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
