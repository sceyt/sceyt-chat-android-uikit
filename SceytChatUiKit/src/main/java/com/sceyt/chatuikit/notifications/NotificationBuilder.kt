package com.sceyt.chatuikit.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.push.PushData

interface NotificationBuilder {
    @DrawableRes
    fun provideNotificationSmallIcon(): Int
    fun provideActions(notificationId: Int, data: PushData): List<NotificationCompat.Action>
    fun providePendingIntent(context: Context, data: PushData): PendingIntent
    suspend fun provideAvatarIcon(context: Context, pushData: PushData): IconCompat?
    fun buildNotification(builder: NotificationCompat.Builder, pushData: PushData): Notification
}