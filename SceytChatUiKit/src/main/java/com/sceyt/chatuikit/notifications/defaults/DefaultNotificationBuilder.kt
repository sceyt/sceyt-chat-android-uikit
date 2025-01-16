package com.sceyt.chatuikit.notifications.defaults

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.extensions.getBitmapFromUrl
import com.sceyt.chatuikit.notifications.NotificationBuilder
import com.sceyt.chatuikit.push.PushData
import kotlin.random.Random

open class DefaultNotificationBuilder : NotificationBuilder {
    override fun provideActions(
            notificationId: Int,
            data: PushData
    ): List<NotificationCompat.Action> {
        return emptyList()
    }

    override fun providePendingIntent(
            context: Context,
            data: PushData
    ): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(context, Random.nextInt(), intent, pendingIntentFlags)
    }

    override suspend fun provideAvatarIcon(
            context: Context,
            pushData: PushData
    ): IconCompat? {
        return pushData.user.avatarURL?.let { url ->
            getBitmapFromUrl(url)?.let { IconCompat.createWithAdaptiveBitmap(it) }
        }
    }

    override fun buildNotification(builder: NotificationCompat.Builder, pushData: PushData): Notification {
        return builder.build()
    }

    protected open val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    } else PendingIntent.FLAG_UPDATE_CURRENT
}