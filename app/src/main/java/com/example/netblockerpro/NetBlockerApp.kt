package com.example.netblockerpro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for NetBlocker Pro.
 * Initializes Hilt dependency injection.
 */
@HiltAndroidApp
class NetBlockerApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Hilt handles initialization automatically
    }
}
