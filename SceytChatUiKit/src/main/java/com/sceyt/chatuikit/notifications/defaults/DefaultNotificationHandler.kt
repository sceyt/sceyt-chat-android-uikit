package com.sceyt.chatuikit.notifications.defaults

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.notifications.NotificationHandler
import com.sceyt.chatuikit.push.PushData

open class DefaultNotificationHandler : NotificationHandler {
    protected val personsMap = mutableMapOf<String, Person>()

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun showNotification(context: Context, data: PushData) {
        val notificationManager = NotificationManagerCompat.from(context)
        val channel = data.channel
        val notificationId = channel.id.toInt()

        val messagingStyle = notificationManager.extractMessagingStyle(notificationId)
                ?: data.createMessagingStyle(context)
        messagingStyle.addMessage(data.toNotificationStyle(context))

        val notificationBuilder = NotificationCompat.Builder(context, context.getNotificationChannelId())
            .setSmallIcon(R.drawable.sceyt_ic_notification_small_icon)
            .setStyle(messagingStyle)
            .setContentIntent(notifications.notificationBuilder.providePendingIntent(context, data))
            .apply {
                notifications.notificationBuilder.provideActions(
                    notificationId = notificationId,
                    data = data
                ).forEach(::addAction)
            }
        val notification = notifications.notificationBuilder.buildNotification(notificationBuilder, data)
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private suspend fun PushData.toNotificationStyle(context: Context) = with(message) {
        MessagingStyle.Message(
            body,
            createdAt.takeIf { it != 0L } ?: System.currentTimeMillis(),
            getPerson(context),
        )
    }

    private fun Context.getNotificationChannelId(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notifications.notificationChannelBuilder.buildChannel(this)
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
            channel.id
        } else ""
    }

    private fun NotificationManagerCompat.extractMessagingStyle(
            notificationId: Int
    ): MessagingStyle? {
        val notification = activeNotifications.firstOrNull {
            it.id == notificationId
        }?.notification ?: return null
        return MessagingStyle.extractMessagingStyleFromNotification(notification)
    }

    private suspend fun PushData.createMessagingStyle(
            context: Context
    ) = MessagingStyle(getPerson(context))
        .setConversationTitle(channel.channelSubject)

    private suspend fun SceytUser.toPerson(context: Context, pushData: PushData): Person {
        return Person.Builder()
            .setKey(id)
            .setName(SceytChatUIKit.formatters.userNameFormatter.format(context, this))
            .setIcon(notifications.notificationBuilder.provideAvatarIcon(context, pushData))
            .build()
    }

    private suspend fun PushData.getPerson(context: Context) = user.let {
        personsMap.getOrPut(it.id) {
            it.toPerson(context, this)
        }
    }
}