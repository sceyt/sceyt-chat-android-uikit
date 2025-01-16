package com.sceyt.chatuikit.notifications.defaults

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.notifications.NotificationHandler
import com.sceyt.chatuikit.push.PushData

open class DefaultNotificationHandler(
        private val context: Context
) : NotificationHandler {
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
    private val personsMap by lazy { mutableMapOf<String, Person>() }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun showNotification(context: Context, data: PushData) {
        val channel = data.channel
        val notificationId = channel.id.toInt()

        val messagingStyle = notificationManager.extractMessagingStyle(notificationId)
                ?: data.createMessagingStyle()
        messagingStyle.addMessage(data.toNotificationStyle())

        val notificationBuilder = NotificationCompat.Builder(context, context.getNotificationChannelId())
            .setSmallIcon(notifications.notificationBuilder.provideNotificationSmallIcon())
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

    override fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    override fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    private suspend fun PushData.toNotificationStyle() = with(message) {
        MessagingStyle.Message(
            SceytChatUIKit.formatters.notificationBodyFormatter.format(context, this@toNotificationStyle),
            createdAt.takeIf { it != 0L } ?: System.currentTimeMillis(),
            getPerson(),
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

    private suspend fun PushData.createMessagingStyle() = MessagingStyle(getPerson())
        .setConversationTitle(channel.channelSubject)

    private suspend fun SceytUser.toPerson(pushData: PushData): Person {
        return Person.Builder()
            .setKey(id)
            .setName(SceytChatUIKit.formatters.userNameFormatter.format(context, this))
            .setIcon(notifications.notificationBuilder.provideAvatarIcon(context, pushData))
            .build()
    }

    private suspend fun PushData.getPerson() = user.let {
        personsMap.getOrPut(it.id) {
            it.toPerson(this)
        }
    }
}