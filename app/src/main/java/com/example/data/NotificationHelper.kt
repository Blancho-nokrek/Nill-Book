package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationHelper {

    const val CHANNEL_MESSAGES = "nillbook_messages"
    const val CHANNEL_CALLS = "nillbook_calls"
    const val CHANNEL_FRIENDS = "nillbook_friends"
    const val CHANNEL_SYSTEM = "nillbook_system"

    // In-app alert notification bus
    private val _inAppAlerts = MutableSharedFlow<InAppAlert>(extraBufferCapacity = 5)
    val inAppAlerts: SharedFlow<InAppAlert> = _inAppAlerts.asSharedFlow()

    data class InAppAlert(
        val title: String,
        val message: String,
        val type: String = "info" // "message", "call", "friend", "sync"
    )

    fun initializeChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages & Chats",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming chat messages and SMS alerts"
                enableVibration(true)
            }

            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Internet & SIM Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice & video calls"
                enableVibration(true)
            }

            val friendChannel = NotificationChannel(
                CHANNEL_FRIENDS,
                "Friend Requests & Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friend invitations, acceptances, and mutual caller alerts"
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "System & Sync Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Data synchronization, offline mode alerts, and security warnings"
            }

            notificationManager.createNotificationChannels(
                listOf(msgChannel, callChannel, friendChannel, systemChannel)
            )
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_SYSTEM
    ) {
        showNotification(context, channelId, (System.currentTimeMillis() % 100000).toInt(), title, message)
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String
    ) {
        // Emit in-app alert first
        _inAppAlerts.tryEmit(InAppAlert(title, message, channelId))

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return // Permission not yet granted, in-app banner handled
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled safely
        }
    }
}
