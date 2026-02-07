package com.example.netblockerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.netblockerpro.data.local.entity.ConnectionLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for connection logs.
 * Supports paginated queries and bulk operations.
 */
@Dao
interface ConnectionLogDao {
    
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ConnectionLogEntity>>
    
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<ConnectionLogEntity>>
    
    @Query("SELECT * FROM connection_logs WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getLogsForApp(packageName: String): Flow<List<ConnectionLogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLogEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ConnectionLogEntity>)
    
    @Query("DELETE FROM connection_logs")
    suspend fun clearAllLogs()
    
    @Query("DELETE FROM connection_logs WHERE timestamp < :timestamp")
    suspend fun deleteLogsOlderThan(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM connection_logs WHERE wasBlocked = 1")
    fun getTotalBlockedCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM connection_logs WHERE wasBlocked = 1 AND timestamp > :since")
    fun getBlockedCountSince(since: Long): Flow<Int>
}
