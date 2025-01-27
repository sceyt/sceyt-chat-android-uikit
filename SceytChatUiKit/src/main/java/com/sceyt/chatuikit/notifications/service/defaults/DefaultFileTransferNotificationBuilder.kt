package com.sceyt.chatuikit.notifications.service.defaults

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.notifications.builder.FileTransferNotificationBuilder
import com.sceyt.chatuikit.notifications.builder.NotificationBuilder
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.immutablePendingIntentFlags
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData

/**
 * Implementation of [NotificationBuilder] for creating and customizing notifications for file transfers.
 */
open class DefaultFileTransferNotificationBuilder(
        private val context: Context
) : FileTransferNotificationBuilder {
    private val serviceNotifications by lazy { notifications.fileTransferServiceNotification }

    /**
     * Provides the resource ID for the small icon displayed in the notification.
     *
     * @return The resource ID of the small icon (default: [R.drawable.sceyt_ic_upload]).
     */
    override fun provideNotificationSmallIcon(data: FileTransferNotificationData): Int {
        return R.drawable.sceyt_ic_upload
    }

    override suspend fun provideNotificationStyle(
            context: Context,
            data: FileTransferNotificationData,
            notificationId: Int
    ): NotificationCompat.Style? = null

    /**
     * Provides a list of actions to be added to the notification.
     *
     * @param data The push data received for the notification.
     * @return A list of [NotificationCompat.Action] objects (default: empty list).
     */
    override fun provideActions(
            context: Context,
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
            immutablePendingIntentFlags
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
            data: FileTransferNotificationData,
            notificationId: Int,
            builderCustomizer: NotificationCompat.Builder.() -> Unit
    ): Notification {
        val style = provideNotificationStyle(context, data, notificationId)
        return buildNotificationImpl(context, data, notificationId, style)
    }

    override suspend fun buildNotification(
            context: Context,
            data: FileTransferNotificationData,
            notificationId: Int,
            style: NotificationCompat.Style?,
            builderCustomizer: NotificationCompat.Builder.() -> Unit
    ): Notification {
        return buildNotificationImpl(context, data, notificationId, style)
    }

    protected open fun buildNotificationImpl(
            context: Context,
            data: FileTransferNotificationData,
            notificationId: Int,
            style: NotificationCompat.Style?
    ): Notification {
        return NotificationCompat.Builder(context, provideNotificationChannelId())
            .setSmallIcon(provideNotificationSmallIcon(data))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(providePendingIntent(context, data))
            .setStyle(style)
            .apply {
                provideActions(context, data).forEach(::addAction)
            }
            .build()
    }

    protected open fun provideNotificationChannelId(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceNotifications.notificationChannelProvider.createChannel(
                context = context
            ).id
        } else ""
    }
}
