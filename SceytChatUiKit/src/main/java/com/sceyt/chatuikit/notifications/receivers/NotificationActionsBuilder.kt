package com.sceyt.chatuikit.notifications.receivers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.immutablePendingIntentFlags
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.mutablePendingIntentFlags
import com.sceyt.chatuikit.push.PushData

object NotificationActionsBuilder {
    const val ACTION_READ = "sceyt_action_mark_us_read"
    const val ACTION_REPLY = "sceyt_action_reply"

    // Keys
    const val KEY_PUSH_DATA = "push_data"
    const val KEY_REPLY_TEXT = "reply_text"

    fun createReadAction(context: Context, data: PushData): NotificationCompat.Action {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            data.channel.id.toInt(),
            createActionIntent(context, data, ACTION_READ),
            immutablePendingIntentFlags,
        )
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            context.getString(R.string.sceyt_mark_as_read),
            pendingIntent,
        ).build()
    }

    fun createReplyAction(context: Context, data: PushData): NotificationCompat.Action {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            data.channel.id.toInt(),
            createActionIntent(context, data, ACTION_REPLY),
            mutablePendingIntentFlags,
        )
        val remoteInput =
                RemoteInput.Builder(KEY_REPLY_TEXT)
                    .setLabel(context.getString(R.string.sceyt_write_a_message))
                    .build()
        return NotificationCompat.Action.Builder(
            R.drawable.sceyt_ic_send_message,
            context.getString(R.string.sceyt_reply),
            pendingIntent
        ).apply {
            addRemoteInput(remoteInput)
            setAllowGeneratedReplies(true)
        }.build()
    }

    private fun createActionIntent(context: Context, data: PushData, action: String): Intent {
        return Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(KEY_PUSH_DATA, data)
            this.action = action
        }
    }
}