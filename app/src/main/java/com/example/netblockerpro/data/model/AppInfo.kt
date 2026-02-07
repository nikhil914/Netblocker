package com.example.netblockerpro.data.model

import android.graphics.drawable.Drawable

/**
 * Domain model representing an installed app with its firewall rules.
 * Used for UI display in the app list.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isWifiBlocked: Boolean = false,
    val isDataBlocked: Boolean = false,
    val uid: Int = 0
)
