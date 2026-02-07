package com.example.netblockerpro.data.repository

import com.example.netblockerpro.data.local.dao.ConnectionLogDao
import com.example.netblockerpro.data.local.entity.ConnectionLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for connection logs.
 */
@Singleton
class LogRepository @Inject constructor(
    private val connectionLogDao: ConnectionLogDao
) {
    
    fun getAllLogs(): Flow<List<ConnectionLogEntity>> {
        return connectionLogDao.getAllLogs()
    }
    
    fun getRecentLogs(limit: Int = 100): Flow<List<ConnectionLogEntity>> {
        return connectionLogDao.getRecentLogs(limit)
    }
    
    fun getLogsForApp(packageName: String): Flow<List<ConnectionLogEntity>> {
        return connectionLogDao.getLogsForApp(packageName)
    }
    
    suspend fun logConnection(log: ConnectionLogEntity) {
        connectionLogDao.insertLog(log)
    }
    
    suspend fun logConnections(logs: List<ConnectionLogEntity>) {
        connectionLogDao.insertLogs(logs)
    }
    
    suspend fun clearAllLogs() {
        connectionLogDao.clearAllLogs()
    }
    
    suspend fun deleteOldLogs(olderThanDays: Int = 7) {
        val cutoff = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        connectionLogDao.deleteLogsOlderThan(cutoff)
    }
    
    fun getTotalBlockedCount(): Flow<Int> {
        return connectionLogDao.getTotalBlockedCount()
    }
    
    fun getBlockedCountToday(): Flow<Int> {
        val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        return connectionLogDao.getBlockedCountSince(startOfDay)
    }
}
