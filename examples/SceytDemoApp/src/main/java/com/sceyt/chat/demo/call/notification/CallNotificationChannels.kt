package com.sceyt.chat.demo.call.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * Manages notification channels for call-related notifications.
 */
object CallNotificationChannels {

    /** High-importance channel — used when app is in background (heads-up + full-screen intent). */
    const val INCOMING_CALL_CHANNEL_ID = "sceyt_incoming_call"
    /** Low-importance channel — used when app is in foreground (no heads-up, call UI opens directly). */
    const val INCOMING_CALL_SILENT_CHANNEL_ID = "sceyt_incoming_call_silent"
    const val ONGOING_CALL_CHANNEL_ID = "sceyt_ongoing_call"

    const val CALL_NOTIFICATION_ID = 10001

    /**
     * Creates notification channels for calls.
     * Must be called before posting any call notifications.
     */
    fun createChannels(context: Context) {
        val notificationManager = context.getSystemService<NotificationManager>() ?: return

        // Background incoming call channel — IMPORTANCE_HIGH so heads-up and full-screen intent work
        val incomingChannel = NotificationChannel(
            INCOMING_CALL_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming voice and video calls"
            // ToneManager handles both ringtone and vibration via playRingtoneAndVibrate()
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }

        // Foreground incoming call channel — IMPORTANCE_LOW, app is already visible so no heads-up needed
        val incomingCallSilentChannel = NotificationChannel(
            INCOMING_CALL_SILENT_CHANNEL_ID,
            "Incoming Calls (Silent)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Silent foreground service notification when call UI is already visible"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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

        notificationManager.createNotificationChannels(
            listOf(incomingChannel, incomingCallSilentChannel, ongoingChannel)
        )
    }
}
