package com.sceyt.chatuikit.notifications

import com.sceyt.chatuikit.notifications.defaults.DefaultNotificationBuilder
import com.sceyt.chatuikit.notifications.defaults.DefaultNotificationChannelBuilder
import com.sceyt.chatuikit.notifications.defaults.DefaultNotificationHandler

data class SceytNotifications(
        val notificationHandler: NotificationHandler = DefaultNotificationHandler(),
        val notificationChannelBuilder: NotificationChannelBuilder = DefaultNotificationChannelBuilder(),
        val notificationBuilder: NotificationBuilder = DefaultNotificationBuilder(),
)
