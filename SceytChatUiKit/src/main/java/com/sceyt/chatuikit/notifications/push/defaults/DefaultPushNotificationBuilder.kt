package com.sceyt.chatuikit.notifications.push.defaults

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getBitmapFromUrl
import com.sceyt.chatuikit.notifications.PushNotificationBuilder
import com.sceyt.chatuikit.notifications.extractMessagingStyle
import com.sceyt.chatuikit.notifications.toPerson
import com.sceyt.chatuikit.push.PushData

/**
 * Implementation of [PushNotificationBuilder] for creating and customizing notifications.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class DefaultPushNotificationBuilder(
        private val context: Context
) : PushNotificationBuilder {
    protected val notificationManager by lazy { NotificationManagerCompat.from(context) }
    protected val personMap by lazy { mutableMapOf<String, Person>() }

    companion object {
        const val EXTRAS_NOTIFICATION_TYPE = "type"
        const val EXTRAS_MESSAGE_ID = "messageId"
    }

    override fun provideNotificationSmallIcon(data: PushData): Int {
        return R.drawable.sceyt_ic_notification_small_icon
    }

    override suspend fun provideNotificationStyle(
            context: Context,
            data: PushData,
            notificationId: Int
    ): NotificationCompat.Style? = notificationManager.extractMessagingStyle(notificationId)?.run {
        addMessage(data.toMessagingStyle())
    } ?: data.createMessagingStyle()

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
            notificationId: Int
    ): Notification {
        val notificationBuilder = NotificationCompat.Builder(context, provideNotificationChannelId())
            .setSmallIcon(provideNotificationSmallIcon(data))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(providePendingIntent(context, data))
            .apply {
                provideNotificationStyle(context, data, notificationId)?.let(::setStyle)
                provideActions(context, data).forEach(::addAction)
            }

        return notificationBuilder.build()
    }

    protected open fun provideNotificationChannelId(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SceytChatUIKit.notifications.pushNotification.notificationChannelProvider.createChannel(
                context = context
            ).id
        } else ""
    }

    protected open suspend fun PushData.toMessagingStyle() = with(message) {
        MessagingStyle.Message(
            SceytChatUIKit.formatters.notificationBodyFormatter.format(context, this@toMessagingStyle),
            createdAt.takeIf { it != 0L } ?: System.currentTimeMillis(),
            getPerson(),
        ).apply {
            extras.putLong(EXTRAS_MESSAGE_ID, message.id)
            extras.putInt(EXTRAS_NOTIFICATION_TYPE, this@toMessagingStyle.type.ordinal)
        }
    }

    protected open suspend fun PushData.createMessagingStyle() = MessagingStyle(getPerson())
        .setConversationTitle(channel.channelSubject)
        .setGroupConversation(channel.isGroup)

    protected open suspend fun PushData.getPerson() = user.let {
        personMap.getOrPut(it.id) {
            it.toPerson(context, provideAvatarIcon(context, this))
        }
    }

    protected open val pendingIntentFlags: Int
        get() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
}
