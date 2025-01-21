package com.sceyt.chatuikit.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.push.PushData

typealias PushNotificationBuilder = NotificationBuilder<PushData>
typealias FileTransferNotificationBuilder = NotificationBuilder<FileTransferNotificationData>

/**
 * Constructs notification objects for a specific type of data.
 *
 * @param T The type of data used to configure and build notifications.
 */
interface NotificationBuilder<T> {

    /**
     * Provides the small icon resource ID for the notification.
     *
     * @param data The data used to determine the icon.
     *
     * @return The resource ID of the small icon.
     */
    @DrawableRes
    fun provideNotificationSmallIcon(data: T): Int

    suspend fun provideNotificationStyle(context: Context, data: T, notificationId: Int): NotificationCompat.Style?

    /**
     * Defines actions to be added to the notification.
     *
     * @param data The data required to create the actions.
     * @return A list of [NotificationCompat.Action] objects.
     */
    fun provideActions(context: Context, data: T): List<NotificationCompat.Action>

    /**
     * Creates a pending intent for the notification.
     *
     * @param context The context used to build the intent.
     * @param data The data required to configure the intent.
     * @return A [PendingIntent] instance.
     */
    fun providePendingIntent(context: Context, data: T): PendingIntent

    /**
     * Provides the avatar icon for the notification, if applicable.
     *
     * @param context The context used to load resources or assets.
     * @param data The data used to generate the icon.
     * @return An [IconCompat] instance or `null` if no avatar is provided.
     */
    suspend fun provideAvatarIcon(context: Context, data: T): IconCompat?

    /**
     * Builds a notification object using the provided data.
     *
     * @param context The context used for accessing resources and services.
     * @param data The data required to build the notification.
     * @param notificationId The ID of the notification to build.
     * @return A fully constructed [Notification] instance.
     */
    suspend fun buildNotification(context: Context, data: T, notificationId: Int): Notification
}