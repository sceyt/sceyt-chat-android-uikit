package com.sceyt.chat.demo.call.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Manages notification channels for call-related notifications.
 */
object CallNotificationChannels {

    const val INCOMING_CALL_CHANNEL_ID = "sceyt_incoming_call"
    const val ONGOING_CALL_CHANNEL_ID = "sceyt_ongoing_call"

    const val INCOMING_CALL_NOTIFICATION_ID = 10001
    const val ONGOING_CALL_NOTIFICATION_ID = 10002

    /**
     * Creates notification channels for calls.
     * Must be called before posting any call notifications.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService<NotificationManager>() ?: return

        // Incoming call channel - high priority for heads-up display
        val incomingChannel = NotificationChannel(
            INCOMING_CALL_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_LOW// todo: change to HIGH when implementing notifications for incoming calls
        ).apply {
            description = "Notifications for incoming voice and video calls"
            // Don't play sound through notification - ToneManager handles ringtone
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }

        // Ongoing call channel - low priority, persistent but quiet
        val ongoingChannel = NotificationChannel(
            ONGOING_CALL_CHANNEL_ID,
            "Ongoing Calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for active calls in progress"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannels(listOf(incomingChannel, ongoingChannel))
    }
}
