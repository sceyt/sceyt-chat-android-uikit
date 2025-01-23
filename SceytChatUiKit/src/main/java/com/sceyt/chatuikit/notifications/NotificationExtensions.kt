package com.sceyt.chatuikit.notifications

import android.content.Context
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder.Companion.EXTRAS_CHAT_NOTIFICATION

fun NotificationManagerCompat.extractMessagingStyle(
        notificationId: Int
): MessagingStyle? {
    val notification = activeNotifications.firstOrNull {
        it.id == notificationId
    }?.notification ?: return null
    return MessagingStyle.extractMessagingStyleFromNotification(notification)
}

fun NotificationManagerCompat.provideAllChatMessagesCount(): Int {
    return activeNotifications.filter {
        it.notification.extras.containsKey(EXTRAS_CHAT_NOTIFICATION)
    }.sumOf {
        val style = MessagingStyle.extractMessagingStyleFromNotification(it.notification)
        style?.messages?.size ?: 1
    }
}

fun SceytUser.toPerson(
        context: Context,
        icon: IconCompat?
): Person {
    return Person.Builder()
        .setKey(id)
        .setName(SceytChatUIKit.formatters.userNameFormatter.format(context, this))
        .setIcon(icon)
        .build()
}
