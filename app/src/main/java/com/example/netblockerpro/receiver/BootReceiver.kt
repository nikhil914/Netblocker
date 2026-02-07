package com.example.netblockerpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.example.netblockerpro.service.NetBlockerVpnService
import com.example.netblockerpro.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = Constants.DATASTORE_NAME)

/**
 * Receiver for boot completed events.
 * Starts the VPN service automatically if enabled in settings.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val autoStartKey = booleanPreferencesKey(Constants.PREF_AUTO_START)
                val autoStart = context.dataStore.data
                    .map { preferences -> preferences[autoStartKey] ?: false }
                    .first()
                
                if (autoStart) {
                    val serviceIntent = Intent(context, NetBlockerVpnService::class.java).apply {
                        action = Constants.ACTION_START_VPN
                    }
                    context.startForegroundService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
