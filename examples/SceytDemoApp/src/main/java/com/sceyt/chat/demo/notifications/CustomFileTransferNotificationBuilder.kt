package com.sceyt.chat.demo.notifications

import android.app.PendingIntent
import android.content.Context
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.notifications.service.defaults.DefaultFileTransferNotificationBuilder
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity

class CustomFileTransferNotificationBuilder(
        context: Context
) : DefaultFileTransferNotificationBuilder(context) {

    override fun providePendingIntent(context: Context, data: FileTransferNotificationData): PendingIntent {
        val intent = ChannelActivity.createIntent(context, data.channel)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, data.channel.id.toInt(), intent, flags)
    }
}
