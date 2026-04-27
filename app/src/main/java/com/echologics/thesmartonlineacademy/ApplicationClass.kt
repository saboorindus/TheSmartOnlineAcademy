package com.echologics.thesmartonlineacademy

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApplicationClass: Application() {

    override fun onCreate() {
        super.onCreate()

        // Replace with your 36-character App ID from Dashboard > Settings > Keys & IDs
        OneSignal.initWithContext(this, getString(R.string.onesignal_app_id))

        // Prompt user for push notification permission
        // In production, consider using an in-app message instead for better opt-in rates
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }

    }
}