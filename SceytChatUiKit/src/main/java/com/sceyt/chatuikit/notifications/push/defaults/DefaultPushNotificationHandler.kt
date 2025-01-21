package com.sceyt.chatuikit.notifications.push.defaults

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.extensions.cancelChannelNotifications
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.notifications.PushNotificationHandler
import com.sceyt.chatuikit.push.PushData

open class DefaultPushNotificationHandler(
        private val context: Context
) : PushNotificationHandler {
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun showNotification(
            context: Context,
            data: PushData,
    ) {
        val notificationId = when (data.type) {
            NotificationType.ChannelMessage -> data.channel.id.toInt()
            NotificationType.MessageReaction -> {
                val user = data.reaction?.user ?: return
                "${data.message.id}_${user.id}_${data.reaction.key}".hashCode()
                data.channel.id.toInt()
            }
        }
        val notification = notifications.pushNotification.notificationBuilder.buildNotification(
            context = context,
            data = data,
            notificationId = notificationId
        )
        notificationManager.notify(notificationId, notification)
    }

    override fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    override fun cancelAllNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = notifications.pushNotification.notificationChannelProvider.createChannel(context).id
            notificationManager.cancelChannelNotifications(channelId)
        }
    }
}