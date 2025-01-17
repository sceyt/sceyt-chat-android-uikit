package com.sceyt.chatuikit.notifications

import android.content.Context
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.push.PushData

typealias PushNotificationHandler = NotificationHandler<PushData>
typealias FileTransferNotificationHandler = NotificationHandler<FileTransferNotificationData>

/**
 * Defines how notifications are managed for a specific type of data.
 *
 * @param T The type of data used to build and display notifications.
 */
interface NotificationHandler<T> {
    /**
     * Displays a notification using the provided data.
     *
     * @param context The context used for accessing resources and services.
     * @param data The data required to display the notification.
     */
    suspend fun showNotification(context: Context, data: T)

    /**
     * Cancels a notification by its ID.
     *
     * @param notificationId The ID of the notification to cancel.
     */
    fun cancelNotification(notificationId: Int)

    /**
     * Cancels all notifications managed by this handler.
     */
    fun cancelAllNotifications()
}