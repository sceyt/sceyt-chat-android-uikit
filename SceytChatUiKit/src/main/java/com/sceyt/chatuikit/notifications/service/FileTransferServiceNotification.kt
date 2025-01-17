package com.sceyt.chatuikit.notifications.service

import android.content.Context
import com.sceyt.chatuikit.notifications.FileTransferNotificationBuilder
import com.sceyt.chatuikit.notifications.FileTransferNotificationHandler
import com.sceyt.chatuikit.notifications.NotificationChannelProvider
import com.sceyt.chatuikit.notifications.service.defaults.DefaultFileTransferNotificationBuilder
import com.sceyt.chatuikit.notifications.service.defaults.DefaultFileTransferNotificationChannelProvider
import com.sceyt.chatuikit.notifications.service.defaults.DefaultFileTransferNotificationHandler
import com.sceyt.chatuikit.persistence.lazyVar

/**
 * Manages notifications for file transfer services, such as uploads.
 *
 * @param context The application context used for initializing components.
 */
class FileTransferServiceNotification(context: Context) {

    /**
     * Handles file transfer notifications.
     * Default: [DefaultFileTransferNotificationHandler].
     */
    @Suppress("unused")
    var notificationHandler: FileTransferNotificationHandler by lazyVar {
        DefaultFileTransferNotificationHandler(context)
    }

    /**
     * Creates and configures the notification channel for file transfers.
     * Default: [DefaultFileTransferNotificationChannelProvider].
     */
    var notificationChannelProvider: NotificationChannelProvider by lazyVar {
        DefaultFileTransferNotificationChannelProvider(context)
    }

    /**
     * Constructs the notification object used for file transfer operations.
     * Default: [DefaultFileTransferNotificationBuilder].
     */
    var notificationBuilder: FileTransferNotificationBuilder by lazyVar {
        DefaultFileTransferNotificationBuilder(context)
    }
}
