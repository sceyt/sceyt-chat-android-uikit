package com.sceyt.chatuikit.notifications.defaults

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.notifications.NotificationChannelBuilder

/**
 * A default implementation of [NotificationChannelBuilder] responsible for creating notification channels.
 * This is specifically designed for devices running Android O (API 26) and above, where notification channels are required.
 */
open class DefaultNotificationChannelBuilder : NotificationChannelBuilder {

    /**
     * Builds a [NotificationChannel] with default settings.
     *
     * @param context The application context used to access resources.
     * @return A [NotificationChannel] instance configured with the default settings.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildChannel(context: Context): NotificationChannel {
        return defaultChannelBuilder(context)
    }

    /**
     * Creates a [NotificationChannel] with the specified importance.
     *
     * @param context The application context used to access resources for channel ID and name.
     * @param importance The importance level of the notifications posted to this channel.
     * @return A configured [NotificationChannel] instance.
     */
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
