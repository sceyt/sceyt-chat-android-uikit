package com.sceyt.chatuikit.notifications

import android.app.NotificationChannel
import android.content.Context

interface NotificationChannelBuilder {
    fun buildChannel(context: Context): NotificationChannel
}