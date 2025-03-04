package com.sceyt.chatuikit.notifications.builder

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.notifications.NotificationChannelProvider
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder
import com.sceyt.chatuikit.notifications.toPerson
import com.sceyt.chatuikit.push.PushData

object NotificationBuilderHelper {
    internal val personMap by lazy { mutableMapOf<String, Person>() }

    fun NotificationChannelProvider.provideNotificationChannelId(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(context = context).id
        } else ""
    }

    fun PushData.toMessagingStyle(
            context: Context,
            person: Person
    ) = NotificationCompat.MessagingStyle.Message(
        SceytChatUIKit.formatters.notificationBodyFormatter.format(context, this),
        message.createdAt.takeIf { it != 0L } ?: System.currentTimeMillis(),
        person,
    ).apply {
        extras.putLong(DefaultPushNotificationBuilder.EXTRAS_MESSAGE_ID, message.id)
        extras.putInt(DefaultPushNotificationBuilder.EXTRAS_NOTIFICATION_TYPE, type.ordinal)
        extras.putBoolean(DefaultPushNotificationBuilder.EXTRAS_CHAT_NOTIFICATION, true)
        reaction?.id?.let { reactionId ->
            extras.putLong(DefaultPushNotificationBuilder.EXTRAS_REACTION_ID, reactionId)
        }
    }

    fun PushData.createMessagingStyle(
            context: Context,
            person: Person
    ): NotificationCompat.MessagingStyle {
        val title = SceytChatUIKit.formatters.notificationTitleFormatter.format(context, this)
        return NotificationCompat.MessagingStyle(person)
            .setConversationTitle(title)
            .setGroupConversation(channel.isGroup)
    }

    fun PushData.getPerson(context: Context, icon: IconCompat?) = user.let {
        personMap.getOrPut(it.id) {
            it.toPerson(context, icon)
        }
    }

    val immutablePendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else PendingIntent.FLAG_UPDATE_CURRENT

    val mutablePendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else PendingIntent.FLAG_UPDATE_CURRENT
}