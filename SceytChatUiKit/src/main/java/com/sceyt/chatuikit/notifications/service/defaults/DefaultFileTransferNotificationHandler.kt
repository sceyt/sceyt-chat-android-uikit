package com.sceyt.chatuikit.notifications.service.defaults

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.notifications.FileTransferNotificationHandler
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.FILE_TRANSFER_NOTIFICATION_ID

@Suppress("MemberVisibilityCanBePrivate")
open class DefaultFileTransferNotificationHandler(
        private val context: Context
) : FileTransferNotificationHandler {
    protected val notificationManager by lazy { NotificationManagerCompat.from(context) }
    protected val serviceNotifications by lazy { notifications.fileTransferServiceNotification }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun showNotification(context: Context, data: FileTransferNotificationData) {
        val notificationId = FILE_TRANSFER_NOTIFICATION_ID
        val notification = serviceNotifications.notificationBuilder.buildNotification(
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
        notificationManager.cancel(FILE_TRANSFER_NOTIFICATION_ID)
    }
}