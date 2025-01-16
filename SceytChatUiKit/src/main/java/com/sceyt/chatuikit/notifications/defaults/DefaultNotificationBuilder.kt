package com.sceyt.chatuikit.notifications.defaults

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getBitmapFromUrl
import com.sceyt.chatuikit.notifications.NotificationBuilder
import com.sceyt.chatuikit.push.PushData

/**
 * Implementation of [NotificationBuilder] for creating and customizing notifications.
 */
open class DefaultNotificationBuilder : NotificationBuilder {

    /**
     * Provides the resource ID for the small icon displayed in the notification.
     *
     * @return The resource ID of the small icon (default: [R.drawable.sceyt_ic_notification_small_icon]).
     */
    override fun provideNotificationSmallIcon(): Int {
        return R.drawable.sceyt_ic_notification_small_icon
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
            data: PushData
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
            data: PushData
    ): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            data.channel.id.toInt(),
            intent,
            pendingIntentFlags
        )
    }

    /**
     * Provides the avatar icon for the notification, based on the user's avatar URL.
     *
     * @param context The application context.
     * @param pushData The push data containing user information.
     * @return An [IconCompat] representing the user's avatar, or `null` if not available.
     */
    override suspend fun provideAvatarIcon(
            context: Context,
            pushData: PushData
    ): IconCompat? {
        return pushData.user.avatarURL?.let { url ->
            getBitmapFromUrl(url)?.let { IconCompat.createWithAdaptiveBitmap(it) }
        }
    }

    /**
     * Builds the final [Notification] using the provided [NotificationCompat.Builder] and push data.
     *
     * @param builder The [NotificationCompat.Builder] used to construct the notification.
     * @param pushData The push data used to customize the notification.
     * @return The constructed [Notification] object.
     */
    override fun buildNotification(
            builder: NotificationCompat.Builder,
            pushData: PushData
    ): Notification {
        return builder.build()
    }

    /**
     * Defines the flags to be used when creating the [PendingIntent].
     */
    protected open val pendingIntentFlags: Int
        get() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
}
