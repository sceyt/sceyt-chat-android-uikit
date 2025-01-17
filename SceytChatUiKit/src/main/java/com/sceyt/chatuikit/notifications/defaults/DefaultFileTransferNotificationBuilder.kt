package com.sceyt.chatuikit.notifications.defaults

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.notifications.FileTransferNotificationBuilder
import com.sceyt.chatuikit.notifications.FileTransferNotificationData
import com.sceyt.chatuikit.notifications.NotificationBuilder

/**
 * Implementation of [NotificationBuilder] for creating and customizing notifications for file transfers.
 */
open class DefaultFileTransferNotificationBuilder(
        protected val context: Context
) : FileTransferNotificationBuilder {
    private val serviceNotifications by lazy { notifications.fileTransferServiceNotification }

    /**
     * Provides the resource ID for the small icon displayed in the notification.
     *
     * @return The resource ID of the small icon (default: [R.drawable.sceyt_ic_upload]).
     */
    override fun provideNotificationSmallIcon(): Int {
        return R.drawable.sceyt_ic_upload
    }

    /**
     * Provides a list of actions to be added to the notification.
     *
     * @param notificationId The unique ID of the notification.
     * @param data The push data received for the notification.
     * @return A list of [NotificationCompat.Action] objects (default: empty list).
     */
    override fun provideActions(
            notificationId: Int,
            data: FileTransferNotificationData
    ): List<NotificationCompat.Action> {
        return emptyList()
    }

    /**
     * Provides the [PendingIntent] that will be triggered when the notification is clicked.
     *
     * @param context The application context.
     * @param data The push data received for the notification.
     * @return A [PendingIntent] to open the app's launcher activity.
     */
    override fun providePendingIntent(
            context: Context,
            data: FileTransferNotificationData
    ): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            "${data.channel.id}${data.message.tid}".hashCode(),
            intent,
            pendingIntentFlags
        )
    }

    /**
     * Provides the avatar icon for the notification.
     *
     * @param context The application context.
     * @param data The push data containing user information.
     * @return An [IconCompat] representing the avatar, or `null` if not available.
     */
    override suspend fun provideAvatarIcon(
            context: Context,
            data: FileTransferNotificationData
    ): IconCompat? {
        return null
    }

    override suspend fun buildNotification(
            context: Context,
            data: FileTransferNotificationData
    ) = NotificationCompat.Builder(context, provideNotificationChannelId())
        .setContentTitle(context.getString(R.string.sceyt_sending_attachment))
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setSmallIcon(serviceNotifications.notificationBuilder.provideNotificationSmallIcon())
        .setContentIntent(serviceNotifications.notificationBuilder.providePendingIntent(
            context = context,
            data = data
        ))
        .apply {
            serviceNotifications.notificationBuilder.provideActions(
                notificationId = data.channel.id.toInt(),
                data = data
            ).forEach(::addAction)
        }
        .build()

    protected open fun provideNotificationChannelId(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceNotifications.notificationChannelProvider.createChannel(
                context = context
            ).id
        } else ""
    }

    /**
     * Defines the flags to be used when creating the [PendingIntent].
     */
    protected open val pendingIntentFlags: Int
        get() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
}
