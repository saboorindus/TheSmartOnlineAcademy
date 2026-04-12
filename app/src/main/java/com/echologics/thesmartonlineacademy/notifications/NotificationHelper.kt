package com.echologics.thesmartonlineacademy.notifications

import android.Manifest
import com.echologics.thesmartonlineacademy.MainActivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BOOKINGS,
                "Booking updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for booking confirmations and payment status"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New message notifications"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SESSIONS,
                "Session reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before your session starts"
            }
        )
    }

    // ── Booking notifications ─────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyBookingConfirmed(context: Context, teacherName: String, subject: String) {
        show(
            context = context,
            channelId = CHANNEL_BOOKINGS,
            notificationId = 1001,
            title = "Booking confirmed!",
            body = "Your session with $teacherName for $subject has been confirmed."
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyPaymentSubmitted(context: Context, studentName: String, amount: String) {
        show(
            context = context,
            channelId = CHANNEL_BOOKINGS,
            notificationId = 1002,
            title = "Payment received from $studentName",
            body = "$studentName has submitted payment of $amount. Please verify and confirm."
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyPaymentConfirmed(context: Context, teacherName: String) {
        show(
            context = context,
            channelId = CHANNEL_BOOKINGS,
            notificationId = 1003,
            title = "Payment verified",
            body = "$teacherName has confirmed your payment. Your session is now confirmed!"
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyBookingRequest(context: Context, studentName: String, subject: String) {
        show(
            context = context,
            channelId = CHANNEL_BOOKINGS,
            notificationId = 1004,
            title = "New booking request",
            body = "$studentName wants to book a $subject session with you."
        )
    }

    // ── Message notifications ─────────────────────────────────────────────────
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyNewMessage(context: Context, senderName: String, messagePreview: String) {
        show(
            context = context,
            channelId = CHANNEL_MESSAGES,
            notificationId = 2001,
            title = senderName,
            body = messagePreview
        )
    }

    // ── Session reminders ─────────────────────────────────────────────────────
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifySessionStartingSoon(
        context: Context,
        otherName: String,
        minutesUntil: Int
    ) {
        show(
            context = context,
            channelId = CHANNEL_SESSIONS,
            notificationId = 3001,
            title = "Session starting in $minutesUntil minutes",
            body = "Your session with $otherName is about to begin. Tap to join."
        )
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifySessionStarted(context: Context, otherName: String) {
        show(
            context = context,
            channelId = CHANNEL_SESSIONS,
            notificationId = 3002,
            title = "Session started",
            body = "Your session with $otherName has started. Tap to join now."
        )
    }

    // ── Core show helper ──────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun show(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                when (channelId) {
                    CHANNEL_SESSIONS, CHANNEL_BOOKINGS -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silently ignore
        }
    }
}