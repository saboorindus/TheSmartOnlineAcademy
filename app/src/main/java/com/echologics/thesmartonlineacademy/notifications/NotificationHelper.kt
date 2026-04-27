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

    private const val CHANNEL_BOOKINGS = "bookings"
    private const val CHANNEL_MESSAGES = "messages"
    private const val CHANNEL_SESSIONS = "sessions"

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Sound setup — shared across channels
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BOOKINGS,
                "Booking updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for booking confirmations and payment status"
                enableLights(true)
                enableVibration(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes) // 👈 add this
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                enableLights(true)
                enableVibration(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes) // 👈 add this
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SESSIONS,
                "Session reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before your session starts"
                enableLights(true)
                enableVibration(true)
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes) // 👈 add this
            }
        )
    }
}