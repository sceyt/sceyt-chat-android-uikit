package com.sceyt.chat.demo.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.push.PushData

class CustomPushNotificationBuilder(
        context: Context
) : DefaultPushNotificationBuilder(context) {

    override fun providePendingIntent(context: Context, data: PushData): PendingIntent {
        val intent = Intent(context, ChannelActivity::class.java).apply {
            putExtra(ChannelActivity.CHANNEL, data.channel)
        }
        return PendingIntent.getActivity(context, data.channel.id.toInt(), intent, pendingIntentFlags)
    }
}