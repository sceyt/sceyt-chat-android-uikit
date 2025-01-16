package com.sceyt.chatuikit.notifications.defaults

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.notifications.NotificationChannelBuilder

open class DefaultNotificationChannelBuilder : NotificationChannelBuilder {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        return defaultChannelBuilder(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun defaultChannelBuilder(
            context: Context,
            importance: Int = NotificationManager.IMPORTANCE_HIGH
    ) = NotificationChannel(
        context.getString(R.string.sceyt_chat_notifications_channel_id),
        context.getString(R.string.sceyt_chat_notifications_channel_name),
        importance
    )
}