package com.sceyt.chat.demo.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.notifications.service.defaults.DefaultFileTransferNotificationBuilder
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity

class CustomFileTransferNotificationBuilder(
        context: Context
) : DefaultFileTransferNotificationBuilder(context) {

    override fun providePendingIntent(context: Context, data: FileTransferNotificationData): PendingIntent {
        val intent = Intent(context, ChannelActivity::class.java).apply {
            putExtra(ChannelActivity.CHANNEL, data.channel)
        }
        return PendingIntent.getActivity(context, data.channel.id.toInt(), intent, pendingIntentFlags)
    }
}