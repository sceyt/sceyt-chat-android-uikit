package com.sceyt.chatuikit.notifications.service.defaults

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.notifications.NotificationChannelProvider

/**
 * A default implementation of [NotificationChannelProvider] responsible for creating notification channels.
 * This is specifically designed for devices running Android O (API 26) and above, where notification channels are required.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class DefaultFileTransferNotificationChannelProvider(
        private val context: Context
) : NotificationChannelProvider {
    protected val notificationManager by lazy { NotificationManagerCompat.from(context) }
    protected var notificationChannel: NotificationChannel? = null

    /**
     * Builds a [NotificationChannel] with default settings.
     *
     * @param context The application context used to access resources.
     * @return A [NotificationChannel] instance configured with the default settings.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context): NotificationChannel {
        return notificationChannel ?: createNotificationChannel().also {
            notificationChannel = it
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected open fun createNotificationChannel(): NotificationChannel {
        val channel = NotificationChannel(
            context.getString(R.string.sceyt_file_transfer_notifications_channel_id),
            context.getString(R.string.sceyt_file_transfer_notifications_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
        return channel
    }
}
