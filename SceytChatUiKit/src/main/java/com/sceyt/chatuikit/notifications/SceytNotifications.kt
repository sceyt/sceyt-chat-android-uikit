package com.sceyt.chatuikit.notifications

import android.content.Context
import com.sceyt.chatuikit.notifications.defaults.DefaultNotificationBuilder
import com.sceyt.chatuikit.notifications.defaults.DefaultNotificationChannelBuilder
import com.sceyt.chatuikit.notifications.defaults.DefaultNotificationHandler
import com.sceyt.chatuikit.persistence.lazyVar

/**
 * A class responsible for managing notifications in the application.
 * Provides customizable handlers and builders for notification behavior.
 */
class SceytNotifications(
        private val context: Context
) {
    /**
     * The handler responsible for processing incoming notifications.
     * This property can be customized to define custom notification handling logic.
     *
     * Default value: [DefaultNotificationHandler].
     */
    var notificationHandler: NotificationHandler by lazyVar {
        DefaultNotificationHandler(context)
    }

    /**
     * The builder responsible for creating notification channels.
     * This allows for customizing channel creation logic as needed.
     *
     * Default value: [DefaultNotificationChannelBuilder].
     */
    var notificationChannelBuilder: NotificationChannelBuilder by lazyVar {
        DefaultNotificationChannelBuilder()
    }

    /**
     * The builder responsible for constructing notification objects.
     * This allows for customizing notification appearance and behavior.
     *
     * Default value: [DefaultNotificationBuilder].
     */
    var notificationBuilder: NotificationBuilder by lazyVar {
        DefaultNotificationBuilder()
    }
}
