package com.sceyt.chat.demo.call.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Manages notification channels for call-related notifications.
 */
object CallNotificationChannels {

    /** High-importance channel — used when app is in background (heads-up + full-screen intent). */
    const val HIGH_PRIORITY_CALL_CHANNEL_ID = "sceyt_incoming_call"

    /** Low-importance channel — used when app is in foreground (no heads-up, call UI opens directly). */
    const val LOW_PRIORITY_CALL_CHANNEL_ID = "sceyt_incoming_call_silent"
    const val CALL_NOTIFICATION_ID = 10001

    /**
     * Creates notification channels for calls.
     * Must be called before posting any call notifications.
     */
    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        // Background call channel — IMPORTANCE_HIGH so heads-up and full-screen intent work
        val incomingChannel = NotificationChannelCompat.Builder(
            HIGH_PRIORITY_CALL_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        )
            .setName("Calls (High Priority)")
            .setDescription("Notifications for incoming voice and video calls")
            .setSound(null, null)
            .setVibrationEnabled(false)
            .build()


        // Foreground call channel — IMPORTANCE_LOW, app is already visible so no heads-up needed
        val incomingCallSilentChannel = NotificationChannelCompat.Builder(
            LOW_PRIORITY_CALL_CHANNEL_ID,
            NotificationManager.IMPORTANCE_LOW
        ).setName("Calls (Silent)")
            .setDescription("Silent foreground service notification when call UI is already visible")
            .setSound(null, null)
            .setVibrationEnabled(false)
            .build()

        notificationManager.createNotificationChannelsCompat(
            listOf(incomingChannel, incomingCallSilentChannel)
        )
    }
}
