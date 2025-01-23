package com.sceyt.chatuikit.notifications

import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Interface for building notification channels.
 *
 * Implementations of this interface are responsible for creating and configuring notification channels,
 * which are required for managing notifications on Android O (API 26) and above.
 */
interface NotificationChannelProvider {

    /**
     * Creates a [NotificationChannel] with the specified configuration.
     *
     * @param context The application context used for accessing resources and system services.
     * @return A [NotificationChannel] instance configured according to the implementation.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(context: Context): NotificationChannel
}
