package com.echologics.thesmartonlineacademy.notifications

import android.Manifest
import com.echologics.thesmartonlineacademy.MainActivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.echologics.thesmartonlineacademy.R

object NotificationHelper {

    private const val CHANNEL_GENERAL = "app_notifications"


    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_GENERAL,
            "App Notifications",
            NotificationManager.IMPORTANCE_HIGH // 👈 REQUIRED for heads-up
        ).apply {
            description = "All app notifications"
            enableLights(true)
            enableVibration(true)
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes) // 👈 sound
        }

        manager.createNotificationChannel(channel)
    }
}