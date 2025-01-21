package com.sceyt.chatuikit.notifications.push.defaults

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getBitmapFromUrl
import com.sceyt.chatuikit.notifications.extractMessagingStyle
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.createMessagingStyle
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.getPerson
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.provideNotificationChannelId
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.toMessagingStyle
import com.sceyt.chatuikit.notifications.builder.PushNotificationBuilder
import com.sceyt.chatuikit.push.PushData

/**
 * Implementation of [PushNotificationBuilder] for creating and customizing notifications.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class DefaultPushNotificationBuilder(
        private val context: Context
) : PushNotificationBuilder {
    protected val notificationManager by lazy { NotificationManagerCompat.from(context) }
    protected val channelProvider by lazy {
        SceytChatUIKit.notifications.pushNotification.notificationChannelProvider
    }

    companion object {
        const val EXTRAS_NOTIFICATION_TYPE = "type"
        const val EXTRAS_MESSAGE_ID = "messageId"
        const val EXTRAS_REACTION_ID = "reactionId"
    }

    override fun provideNotificationSmallIcon(data: PushData): Int {
        return R.drawable.sceyt_ic_notification_small_icon
    }

    override suspend fun provideNotificationStyle(
            context: Context,
            data: PushData,
            notificationId: Int
    ): NotificationCompat.Style? {
        val person = data.getPerson(context, provideAvatarIcon(context, data))
        return (notificationManager.extractMessagingStyle(notificationId)
                ?: data.createMessagingStyle(person)
                ).addMessage(data.toMessagingStyle(context, person))
    }

    override fun provideActions(context: Context, data: PushData): List<NotificationCompat.Action> {
        return emptyList()
    }

    override fun providePendingIntent(
            context: Context,
            data: PushData
    ): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val requestCode = data.channel.id.toInt()
        return PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags)
    }

    override suspend fun provideAvatarIcon(
            context: Context,
            data: PushData
    ): IconCompat? {
        return data.user.avatarURL?.let { url ->
            getBitmapFromUrl(url)?.let { IconCompat.createWithAdaptiveBitmap(it) }
        }
    }

    override suspend fun buildNotification(
            context: Context,
            data: PushData,
            notificationId: Int,
            builderModifier: NotificationCompat.Builder.() -> Unit
    ): Notification {
        val style = provideNotificationStyle(context, data, notificationId)
        return buildNotificationImpl(context, data, notificationId, style, builderModifier)
    }

    override suspend fun buildNotification(
            context: Context,
            data: PushData,
            notificationId: Int,
            style: NotificationCompat.Style?,
            builderModifier: NotificationCompat.Builder.() -> Unit
    ) = buildNotificationImpl(context, data, notificationId, style, builderModifier)

    protected open suspend fun buildNotificationImpl(
            context: Context,
            data: PushData,
            notificationId: Int,
            style: NotificationCompat.Style?,
            builderModifier: NotificationCompat.Builder.() -> Unit
    ): Notification {
        val channelId = channelProvider.provideNotificationChannelId(context)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(provideNotificationSmallIcon(data))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(providePendingIntent(context, data))
            .apply {
                style?.let(::setStyle)
                provideActions(context, data).forEach(::addAction)
                builderModifier()
            }

        return notificationBuilder.build()
    }

    protected open val pendingIntentFlags: Int
        get() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
}
