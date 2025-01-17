package com.sceyt.chatuikit.notifications.defaults

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.notifications.FileTransferNotificationData
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
        notificationManager.cancelAll()
    }
}