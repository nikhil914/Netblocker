package com.example.netblockerpro.ui.screen.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.netblockerpro.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = Constants.DATASTORE_NAME)

/**
 * Theme mode options.
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/**
 * UI state for settings screen.
 */
data class SettingsUiState(
    val autoStartEnabled: Boolean = false,
    val blockScreenOffEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val autoStartKey = booleanPreferencesKey(Constants.PREF_AUTO_START)
    private val blockScreenOffKey = booleanPreferencesKey(Constants.PREF_BLOCK_SCREEN_OFF)
    private val themeModeKey = stringPreferencesKey(Constants.PREF_DARK_MODE)
    
    val uiState: StateFlow<SettingsUiState> = context.dataStore.data
        .map { preferences ->
            SettingsUiState(
                autoStartEnabled = preferences[autoStartKey] ?: false,
                blockScreenOffEnabled = preferences[blockScreenOffKey] ?: false,
                themeMode = preferences[themeModeKey]?.let { 
                    ThemeMode.valueOf(it) 
                } ?: ThemeMode.SYSTEM
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )
    
    fun setAutoStart(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[autoStartKey] = enabled
            }
        }
    }
    
    fun setBlockScreenOff(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[blockScreenOffKey] = enabled
            }
        }
    }
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[themeModeKey] = mode.name
            }
        }
    }
}
