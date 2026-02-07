package com.example.netblockerpro.util

/**
 * App-wide constants.
 */
object Constants {
    
    // Notification
    const val VPN_NOTIFICATION_CHANNEL_ID = "netblocker_vpn_channel"
    const val VPN_NOTIFICATION_CHANNEL_NAME = "Firewall Service"
    const val VPN_NOTIFICATION_ID = 1001
    
    // VPN
    const val VPN_MTU = 1500
    const val VPN_ADDRESS_V4 = "10.0.0.2"
    const val VPN_ROUTE_V4 = "0.0.0.0"
    const val VPN_ADDRESS_V6 = "fd00:1:fd00:1:fd00:1:fd00:1"
    const val VPN_ROUTE_V6 = "::"
    
    // Intents
    const val ACTION_START_VPN = "com.example.netblockerpro.START_VPN"
    const val ACTION_STOP_VPN = "com.example.netblockerpro.STOP_VPN"
    
    // DataStore
    const val DATASTORE_NAME = "netblocker_preferences"
    
    // Preferences Keys
    const val PREF_VPN_ENABLED = "vpn_enabled"
    const val PREF_AUTO_START = "auto_start"
    const val PREF_BLOCK_SCREEN_OFF = "block_screen_off"
    const val PREF_DARK_MODE = "dark_mode"
    const val PREF_HIDE_SYSTEM_APPS = "hide_system_apps"
}
