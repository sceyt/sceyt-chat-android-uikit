package com.sceyt.chat.demo.notifications

import android.app.PendingIntent
import android.content.Context
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.immutablePendingIntentFlags
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.push.PushData

class CustomPushNotificationBuilder(
    context: Context
) : DefaultPushNotificationBuilder(context) {

    override fun providePendingIntent(context: Context, data: PushData): PendingIntent {
        val intent = ChannelActivity.createIntent(context, data.channel)
        return PendingIntent.getActivity(
            context,
            data.channel.id.toInt(),
            intent,
            immutablePendingIntentFlags
        )
    }
}