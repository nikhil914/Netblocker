package com.example.netblockerpro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.netblockerpro.data.local.dao.AppRuleDao
import com.example.netblockerpro.data.local.dao.ConnectionLogDao
import com.example.netblockerpro.data.local.entity.AppRuleEntity
import com.example.netblockerpro.data.local.entity.ConnectionLogEntity

/**
 * Room database for NetBlocker Pro.
 * Contains app rules and connection logs tables.
 */
@Database(
    entities = [
        AppRuleEntity::class,
        ConnectionLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun appRuleDao(): AppRuleDao
    
    abstract fun connectionLogDao(): ConnectionLogDao
    
    companion object {
        const val DATABASE_NAME = "netblocker_db"
    }
}
