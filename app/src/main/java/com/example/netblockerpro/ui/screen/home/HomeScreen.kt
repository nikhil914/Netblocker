package com.example.netblockerpro.ui.screen.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.netblockerpro.service.NetworkMonitor
import com.example.netblockerpro.service.TrafficStatsTracker
import com.example.netblockerpro.service.VpnConnectionManager
import com.example.netblockerpro.ui.components.DashboardCard
import com.example.netblockerpro.ui.components.GlobalControlButtons
import com.example.netblockerpro.ui.screen.home.components.AppListItem
import com.example.netblockerpro.ui.screen.home.components.SearchBar
import com.example.netblockerpro.ui.screen.home.components.SortFilterBar
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    val trafficData by viewModel.trafficData.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpn()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("VPN permission denied")
            }
        }
    }
    
    // Battery optimization launcher
    val batteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check again after returning from settings
        if (viewModel.isBatteryOptimized()) {
            scope.launch {
                snackbarHostState.showSnackbar("Battery optimization still enabled")
            }
        }
    }
    
    fun handleVpnToggle(enabled: Boolean) {
        if (enabled) {
            // Check battery optimization first
            if (viewModel.isBatteryOptimized()) {
                scope.launch {
                    snackbarHostState.showSnackbar("Disable battery optimization for reliable VPN")
                }
                batteryLauncher.launch(viewModel.getBatteryOptimizationIntent())
                return
            }
            
            // Check VPN permission
            val prepareIntent = viewModel.prepareVpn()
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                viewModel.startVpn()
            }
        } else {
            viewModel.stopVpn()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Dashboard
                item {
                    DashboardCard(
                        vpnState = vpnState,
                        networkType = networkType,
                        trafficData = trafficData,
                        blockedAppsCount = uiState.blockedAppsCount,
                        totalBlockedConnections = uiState.totalBlockedConnections,
                        onToggleVpn = ::handleVpnToggle
                    )
                }
                
                // Global controls
                item {
                    GlobalControlButtons(
                        onBlockAllWifi = viewModel::blockAllWifi,
                        onBlockAllData = viewModel::blockAllData
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Search bar
                item {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::setSearchQuery
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Sort/Filter bar
                item {
                    SortFilterBar(
                        currentSort = uiState.sortOption,
                        hideSystemApps = uiState.hideSystemApps,
                        onSortChange = viewModel::setSortOption,
                        onHideSystemChange = viewModel::setHideSystemApps
                    )
                }
                
                // App count
                item {
                    Text(
                        text = "${uiState.filteredApps.size} apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // App list
                items(
                    items = uiState.filteredApps,
                    key = { it.packageName }
                ) { app ->
                    AppListItem(
                        app = app,
                        onWifiToggle = { viewModel.toggleWifiBlocked(app) },
                        onDataToggle = { viewModel.toggleDataBlocked(app) }
                    )
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
