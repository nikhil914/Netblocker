package com.example.netblockerpro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a blocked connection log entry.
 * Records each blocked connection attempt for the Logs screen.
 */
@Entity(tableName = "connection_logs")
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val destinationIp: String,
    val destinationPort: Int,
    val networkType: String, // "WIFI" or "CELLULAR"
    val wasBlocked: Boolean = true
)
