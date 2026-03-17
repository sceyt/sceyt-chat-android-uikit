package com.sceyt.chat.demo.call.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sceyt.chat.call.R
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
        private const val ACTION_DECLINE = "com.sceyt.call.ACTION_DECLINE"
        private const val ACTION_END_CALL = "com.sceyt.call.ACTION_END_CALL"
        private const val ACTION_TOGGLE_MUTE = "com.sceyt.call.ACTION_TOGGLE_MUTE"
    }

    /**
     * Builds a notification for incoming call.
     */
    /**
     * @param suppressFullScreenIntent When true (app in foreground), use low priority without
     *   full-screen intent — the UI is already open. When false (app in background), use high
     *   priority with full-screen intent to surface the incoming call to the user.
     */
    fun buildIncomingCallNotification(
        callerName: String,
        isVideo: Boolean,
        suppressFullScreenIntent: Boolean
    ): Notification {
        val callType = if (isVideo) "Video call" else "Voice call"

        // Content intent — tap notification to open call screen
        val contentIntent = CallActivity.createIncomingIntent(context)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1,
            contentIntent,
            immutablePendingIntentFlags()
        )

        // Action: Answer — must use getActivity() because background activity starts from a
        // BroadcastReceiver are blocked on Android 10+. CallActivity reads EXTRA_AUTO_ANSWER
        // and calls viewModel.onAnswerClick() immediately on launch.
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            2,
            CallActivity.createAnswerIntent(context),
            immutablePendingIntentFlags()
        )

        // Action: Decline
        val declineIntent = Intent(context, CallBroadcastReceiver::class.java).apply {
            action = ACTION_DECLINE
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            declineIntent,
            immutablePendingIntentFlags()
        )

        val channelId = if (suppressFullScreenIntent) {
            CallNotificationChannels.LOW_PRIORITY_CALL_CHANNEL_ID
        } else {
            CallNotificationChannels.HIGH_PRIORITY_CALL_CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_call_up_blue)
            .setContentTitle(callerName)
            .setContentText("Incoming $callType")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_call_up_blue, "Answer", answerPendingIntent)
            .addAction(R.drawable.ic_call_up_blue, "Decline", declinePendingIntent)

        if (suppressFullScreenIntent) {
            // App is in foreground — CallActivity is already launching; keep notification silent
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        } else {
            // App is in background — surface the call with heads-up and full-screen intent
            val fullScreenIntent = CallActivity.createIncomingIntent(context)
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                immutablePendingIntentFlags()
            )
            builder
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(fullScreenPendingIntent, true)
        }

        return builder.build()
    }

    /**
     * Builds a notification for ongoing call.
     */
    fun buildOngoingCallNotification(
        title: String,
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

        return NotificationCompat.Builder(
            context,
            CallNotificationChannels.LOW_PRIORITY_CALL_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_call_up_blue)
            .setContentTitle("$title$muteStatus")
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
        title: String,
        statusText: String,
    ): Notification {
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

        return NotificationCompat.Builder(
            context,
            CallNotificationChannels.LOW_PRIORITY_CALL_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_call_up_blue)
            .setContentTitle(title)
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
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
