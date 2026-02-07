package com.example.netblockerpro.ui.screen.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.netblockerpro.data.local.entity.ConnectionLogEntity
import com.example.netblockerpro.data.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Display model for a log entry.
 */
data class LogDisplayItem(
    val id: Long,
    val appName: String,
    val destinationIp: String,
    val port: Int,
    val networkType: String,
    val formattedTime: String,
    val wasBlocked: Boolean
)

/**
 * UI state for logs screen.
 */
data class LogsUiState(
    val logs: List<LogDisplayItem> = emptyList(),
    val isLoading: Boolean = true,
    val totalCount: Int = 0
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {
    
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    
    val uiState: StateFlow<LogsUiState> = logRepository.getRecentLogs(500)
        .map { logs ->
            LogsUiState(
                logs = logs.map { it.toDisplayItem() },
                isLoading = false,
                totalCount = logs.size
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LogsUiState()
        )
    
    private fun ConnectionLogEntity.toDisplayItem(): LogDisplayItem {
        return LogDisplayItem(
            id = id,
            appName = appName,
            destinationIp = destinationIp,
            port = destinationPort,
            networkType = networkType,
            formattedTime = dateFormat.format(Date(timestamp)),
            wasBlocked = wasBlocked
        )
    }
    
    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
        }
    }
}
