package com.echologics.thesmartonlineacademy.services

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.echologics.thesmartonlineacademy.R

class ScreenCaptureService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "bookings")
            .setContentTitle("Screen sharing active")
            .setContentText("Your screen is being shared in the session")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ requires foreground service type
                startForeground(
                    101,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                // API 26-28
                startForeground(101, notification)
            }
        }
        // API 24-25: startForeground not needed, service just runs normally

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}