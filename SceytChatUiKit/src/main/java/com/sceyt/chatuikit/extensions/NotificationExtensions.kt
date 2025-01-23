package com.sceyt.chatuikit.extensions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

@RequiresApi(Build.VERSION_CODES.O)
fun NotificationManagerCompat.cancelChannelNotifications(channelId: String) {
    for (notification in activeNotifications) {
        if (notification.notification.channelId == channelId)
            cancel(notification.id)
    }
}