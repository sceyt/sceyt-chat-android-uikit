package com.sceyt.chatuikit.notifications.push.defaults

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.notifications.PushNotificationHandler
import com.sceyt.chatuikit.push.PushData

open class DefaultPushNotificationHandler(
        protected val context: Context
) : PushNotificationHandler {
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun showNotification(context: Context, data: PushData) {
        val notificationId = data.channel.id.toInt()
        val notification = notifications.pushNotification.notificationBuilder.buildNotification(
            context = context,
            data = data
        )
        notificationManager.notify(notificationId, notification)
    }

    override fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    override fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}