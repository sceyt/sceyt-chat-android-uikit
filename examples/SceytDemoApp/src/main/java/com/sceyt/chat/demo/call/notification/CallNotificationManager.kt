package com.sceyt.chat.demo.call.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.call.manager.CallUiState
import com.sceyt.chat.demo.call.receiver.CallBroadcastReceiver
import com.sceyt.chat.demo.call.ui.CallActivity

/**
 * Builds notifications for call states.
 */
class CallNotificationManager(
    private val context: Context
) {

    companion object {
        private const val ACTION_ANSWER = "com.sceyt.call.ACTION_ANSWER"
        private const val ACTION_DECLINE = "com.sceyt.call.ACTION_DECLINE"
        private const val ACTION_END_CALL = "com.sceyt.call.ACTION_END_CALL"
        private const val ACTION_TOGGLE_MUTE = "com.sceyt.call.ACTION_TOGGLE_MUTE"
         const val CALL_ID = "CallId"
    }

    /**
     * Builds a notification for incoming call.
     */
    fun buildIncomingCallNotification(
        callerName: String,
        isVideo: Boolean,
        callId: String
    ): Notification {
        val callType = if (isVideo) "Video call" else "Voice call"

        // Full-screen intent for incoming call
        val fullScreenIntent = CallActivity.createIncomingIntent(context)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            immutablePendingIntentFlags()
        )

        // Content intent
        val contentIntent = CallActivity.createIncomingIntent(context)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1,
            contentIntent,
            immutablePendingIntentFlags()
        )

        // Action: Answer
        val answerIntent = Intent(context, CallBroadcastReceiver::class.java).apply {
            action = ACTION_ANSWER
            putExtra(CALL_ID, callId) // Pass actual call ID if needed
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            answerIntent,
            immutablePendingIntentFlags()
        )

        // Action: Decline
        val declineIntent = Intent(context, CallBroadcastReceiver::class.java).apply {
            action = ACTION_DECLINE
            putExtra(CALL_ID, callId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            declineIntent,
            immutablePendingIntentFlags()
        )

        return NotificationCompat.Builder(context, CallNotificationChannels.INCOMING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_up_blue)
            .setContentTitle(callerName)
            .setContentText("Incoming $callType")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_call_up_blue,
                "Answer",
                answerPendingIntent
            )
            .addAction(
                R.drawable.ic_call_up_blue,
                "Decline",
                declinePendingIntent
            )
            .build()
    }

    /**
     * Builds a notification for ongoing call.
     */
    fun buildOngoingCallNotification(
        remoteName: String,
        duration: String,
        isMuted: Boolean,
        isVideo: Boolean
    ): Notification {
        val callType = if (isVideo) "Video call" else "Voice call"
        val muteStatus = if (isMuted) " (Muted)" else ""

        // Content intent to return to call
        val contentIntent = CallActivity.createOngoingIntent(context)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            4,
            contentIntent,
            immutablePendingIntentFlags()
        )

        // Action: End call
        val endCallIntent = Intent(context, CallBroadcastReceiver::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getBroadcast(
            context,
            5,
            endCallIntent,
            immutablePendingIntentFlags()
        )

        // Action: Toggle mute
        val muteIntent = Intent(context, CallBroadcastReceiver::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            6,
            muteIntent,
            immutablePendingIntentFlags()
        )

        return NotificationCompat.Builder(context, CallNotificationChannels.ONGOING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_up_blue)
            .setContentTitle("$remoteName$muteStatus")
            .setContentText("$callType - $duration")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setUsesChronometer(false)
            .addAction(
                if (isMuted) com.sceyt.chatuikit.R.drawable.sceyt_media_view_ic_arrow_back else R.drawable.ic_video_call_blue,
                if (isMuted) "Unmute" else "Mute",
                mutePendingIntent
            )
            .addAction(
                R.drawable.ic_call_up_blue,
                "End",
                endCallPendingIntent
            )
            .build()
    }

    /**
     * Builds a notification for connecting/reconnecting states.
     */
    fun buildConnectingNotification(
        remoteName: String,
        state: CallUiState
    ): Notification {
        val statusText = when (state) {
            is CallUiState.Connecting -> "Connecting..."
            is CallUiState.Reconnecting -> "Reconnecting... (Attempt ${state.attempt})"
            is CallUiState.Outgoing -> "Calling..."
            else -> "Call in progress"
        }

        // Content intent to return to call
        val contentIntent = CallActivity.createOngoingIntent(context)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            7,
            contentIntent,
            immutablePendingIntentFlags()
        )

        // Action: End call
        val endCallIntent = Intent(context, CallBroadcastReceiver::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getBroadcast(
            context,
            8,
            endCallIntent,
            immutablePendingIntentFlags()
        )

        return NotificationCompat.Builder(context, CallNotificationChannels.ONGOING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_up_blue)
            .setContentTitle(remoteName)
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_call_up_blue,
                "End",
                endCallPendingIntent
            )
            .build()
    }

    private fun immutablePendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
