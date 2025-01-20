package com.sceyt.chatuikit.notifications.service.defaults

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.extensions.cancelChannelNotifications
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.notifications.FileTransferNotificationHandler

@Suppress("MemberVisibilityCanBePrivate")
open class DefaultFileTransferNotificationHandler(
        protected val context: Context
) : FileTransferNotificationHandler {
    protected val notificationManager by lazy { NotificationManagerCompat.from(context) }
    protected val serviceNotifications by lazy { notifications.fileTransferServiceNotification }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun showNotification(context: Context, data: FileTransferNotificationData) {
        val notificationId = data.channel.id.toInt()
        val notification = serviceNotifications.notificationBuilder.buildNotification(context, data)
        notificationManager.notify(notificationId, notification)
    }

    override fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    override fun cancelAllNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = serviceNotifications.notificationChannelProvider.createChannel(context).id
            notificationManager.cancelChannelNotifications(channelId)
        }
    }
}