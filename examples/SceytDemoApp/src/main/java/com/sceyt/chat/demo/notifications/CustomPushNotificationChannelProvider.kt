package com.sceyt.chat.demo.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationChannelProvider

class CustomPushNotificationChannelProvider(
        context: Context
) : DefaultPushNotificationChannelProvider(context) {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context): NotificationChannel {
        return if (context.isAppOnForeground()) {
            NotificationChannel(
                context.getString(R.string.sceyt_chat_notifications_channel_id) + "_silent",
                "${context.getString(R.string.sceyt_chat_notifications_channel_name)} Silent",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
                NotificationManagerCompat.from(context).createNotificationChannel(this)
            }
        } else super.createChannel(context)
    }
}