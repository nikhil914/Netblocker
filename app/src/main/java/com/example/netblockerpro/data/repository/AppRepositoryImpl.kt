package com.example.netblockerpro.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.netblockerpro.data.local.dao.AppRuleDao
import com.example.netblockerpro.data.local.entity.AppRuleEntity
import com.example.netblockerpro.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AppRepository.
 * Combines PackageManager data with Room database rules.
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    private val packageManager: PackageManager,
    private val appRuleDao: AppRuleDao
) : AppRepository {
    
    private val ownPackageName = "com.example.netblockerpro"
    
    override fun getInstalledApps(): Flow<List<AppInfo>> {
        return appRuleDao.getAllRules().map { rules ->
            val rulesMap = rules.associateBy { it.packageName }
            
            getInstalledPackages().mapNotNull { appInfo ->
                val packageName = appInfo.packageName
                if (packageName == ownPackageName) return@mapNotNull null
                
                val rule = rulesMap[packageName]
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = try {
                    packageManager.getApplicationIcon(packageName)
                } catch (e: Exception) {
                    null
                }
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val uid = appInfo.uid
                
                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    isSystemApp = isSystemApp,
                    isWifiBlocked = rule?.isWifiBlocked ?: false,
                    isDataBlocked = rule?.isDataBlocked ?: false,
                    uid = uid
                )
            }.sortedBy { it.appName.lowercase() }
        }
    }
    
    override fun getAllRules(): Flow<List<AppRuleEntity>> {
        return appRuleDao.getAllRules()
    }
    
    override suspend fun updateAppRule(packageName: String, wifiBlocked: Boolean, dataBlocked: Boolean) {
        appRuleDao.updateRule(packageName, wifiBlocked, dataBlocked)
    }
    
    override suspend fun blockAllWifi(blocked: Boolean) {
        appRuleDao.setAllWifiBlocked(blocked)
    }
    
    override suspend fun blockAllData(blocked: Boolean) {
        appRuleDao.setAllDataBlocked(blocked)
    }
    
    override suspend fun getRule(packageName: String): AppRuleEntity? {
        return appRuleDao.getRule(packageName)
    }
    
    override fun getBlockedAppsCount(): Flow<Int> {
        return appRuleDao.getBlockedAppsCount()
    }
    
    override suspend fun syncInstalledApps() {
        withContext(Dispatchers.IO) {
            val installedApps = getInstalledPackages()
            val existingRules = appRuleDao.getBlockedRules().associateBy { it.packageName }
            
            val newRules = installedApps.mapNotNull { appInfo ->
                val packageName = appInfo.packageName
                if (packageName == ownPackageName) return@mapNotNull null
                
                // Keep existing rules, create new ones with defaults
                existingRules[packageName] ?: AppRuleEntity(
                    packageName = packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isWifiBlocked = false,
                    isDataBlocked = false
                )
            }
            
            // Insert all apps (upsert will update existing, insert new)
            val allApps = installedApps.mapNotNull { appInfo ->
                val packageName = appInfo.packageName
                if (packageName == ownPackageName) return@mapNotNull null
                
                val existing = existingRules[packageName]
                AppRuleEntity(
                    packageName = packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isWifiBlocked = existing?.isWifiBlocked ?: false,
                    isDataBlocked = existing?.isDataBlocked ?: false
                )
            }
            
            appRuleDao.upsertRules(allApps)
        }
    }
    
    private fun getInstalledPackages(): List<ApplicationInfo> {
        return try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
