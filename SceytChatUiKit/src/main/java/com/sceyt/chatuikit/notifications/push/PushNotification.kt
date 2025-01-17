package com.sceyt.chatuikit.notifications.push

import android.content.Context
import com.sceyt.chatuikit.notifications.NotificationChannelProvider
import com.sceyt.chatuikit.notifications.PushNotificationBuilder
import com.sceyt.chatuikit.notifications.PushNotificationHandler
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationChannelProvider
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationHandler
import com.sceyt.chatuikit.persistence.lazyVar

/**
 * Manages push notifications, including their channels, builders, and handlers.
 *
 * @param context The application context used for initializing components.
 */
class PushNotification(context: Context) {
    /**
     * Handles how push notifications are displayed and interacted with.
     * Default: [DefaultPushNotificationHandler].
     */
    var pushNotificationHandler: PushNotificationHandler by lazyVar {
        DefaultPushNotificationHandler(context)
    }

    /**
     * Creates notification channels for push notifications.
     * Default: [DefaultPushNotificationChannelProvider].
     */
    var notificationChannelProvider: NotificationChannelProvider by lazyVar {
        DefaultPushNotificationChannelProvider(context)
    }

    /**
     * Builds notification objects for push notifications.
     * Default: [DefaultPushNotificationBuilder].
     */
    var notificationBuilder: PushNotificationBuilder by lazyVar {
        DefaultPushNotificationBuilder(context)
    }
}
