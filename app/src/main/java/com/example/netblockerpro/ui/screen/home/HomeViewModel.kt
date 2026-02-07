package com.example.netblockerpro.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.netblockerpro.data.model.AppInfo
import com.example.netblockerpro.data.repository.AppRepository
import com.example.netblockerpro.data.repository.LogRepository
import com.example.netblockerpro.service.NetworkMonitor
import com.example.netblockerpro.service.TrafficStatsTracker
import com.example.netblockerpro.service.VpnConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sorting options for the app list.
 */
enum class SortOption {
    NAME_ASC,
    BLOCKED_FIRST,
    SYSTEM_LAST
}

/**
 * UI state for the home screen.
 */
data class HomeUiState(
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NAME_ASC,
    val hideSystemApps: Boolean = true,
    val isLoading: Boolean = true,
    val blockedAppsCount: Int = 0,
    val totalBlockedConnections: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val logRepository: LogRepository,
    val vpnConnectionManager: VpnConnectionManager,
    val networkMonitor: NetworkMonitor,
    val trafficStatsTracker: TrafficStatsTracker
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    private val _hideSystemApps = MutableStateFlow(true)
    
    val vpnState = vpnConnectionManager.vpnState
    val trafficData = trafficStatsTracker.trafficData
    val networkType = networkMonitor.currentNetwork
    
    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.getInstalledApps(),
        appRepository.getBlockedAppsCount(),
        logRepository.getTotalBlockedCount(),
        _searchQuery,
        _sortOption,
        _hideSystemApps
    ) { values ->
        val apps = values[0] as List<AppInfo>
        val blockedCount = values[1] as Int
        val totalBlocked = values[2] as Int
        val query = values[3] as String
        val sort = values[4] as SortOption
        val hideSystem = values[5] as Boolean
        
        val filtered = apps
            .filter { app ->
                val matchesQuery = query.isEmpty() || 
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                val matchesSystem = !hideSystem || !app.isSystemApp
                matchesQuery && matchesSystem
            }
            .let { list ->
                when (sort) {
                    SortOption.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
                    SortOption.BLOCKED_FIRST -> list.sortedByDescending { 
                        it.isWifiBlocked || it.isDataBlocked 
                    }
                    SortOption.SYSTEM_LAST -> list.sortedBy { it.isSystemApp }
                }
            }
        
        HomeUiState(
            apps = apps,
            filteredApps = filtered,
            searchQuery = query,
            sortOption = sort,
            hideSystemApps = hideSystem,
            isLoading = false,
            blockedAppsCount = blockedCount,
            totalBlockedConnections = totalBlocked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
    
    init {
        viewModelScope.launch {
            appRepository.syncInstalledApps()
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    fun setHideSystemApps(hide: Boolean) {
        _hideSystemApps.value = hide
    }
    
    fun toggleWifiBlocked(app: AppInfo) {
        viewModelScope.launch {
            appRepository.updateAppRule(
                packageName = app.packageName,
                wifiBlocked = !app.isWifiBlocked,
                dataBlocked = app.isDataBlocked
            )
        }
    }
    
    fun toggleDataBlocked(app: AppInfo) {
        viewModelScope.launch {
            appRepository.updateAppRule(
                packageName = app.packageName,
                wifiBlocked = app.isWifiBlocked,
                dataBlocked = !app.isDataBlocked
            )
        }
    }
    
    fun blockAllWifi() {
        viewModelScope.launch {
            appRepository.blockAllWifi(true)
        }
    }
    
    fun blockAllData() {
        viewModelScope.launch {
            appRepository.blockAllData(true)
        }
    }
    
    fun unblockAllWifi() {
        viewModelScope.launch {
            appRepository.blockAllWifi(false)
        }
    }
    
    fun unblockAllData() {
        viewModelScope.launch {
            appRepository.blockAllData(false)
        }
    }
    
    fun startVpn() {
        vpnConnectionManager.startVpn()
    }
    
    fun stopVpn() {
        vpnConnectionManager.stopVpn()
    }
    
    fun prepareVpn() = vpnConnectionManager.prepareVpn()
    
    fun isBatteryOptimized() = vpnConnectionManager.isBatteryOptimized()
    
    fun getBatteryOptimizationIntent() = vpnConnectionManager.getBatteryOptimizationIntent()
}
