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
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getBitmapFromUrl
import com.sceyt.chatuikit.notifications.PushNotificationBuilder
import com.sceyt.chatuikit.push.PushData

/**
 * Implementation of [PushNotificationBuilder] for creating and customizing notifications.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class DefaultPushNotificationBuilder(
        protected  val context: Context
) : PushNotificationBuilder {
    protected val notificationManager by lazy { NotificationManagerCompat.from(context) }
    protected val personsMap by lazy { mutableMapOf<String, Person>() }

    override fun provideNotificationSmallIcon(): Int {
        return R.drawable.sceyt_ic_notification_small_icon
    }

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
        return PendingIntent.getActivity(
            context,
            data.channel.id.toInt(),
            intent,
            pendingIntentFlags
        )
    }

    override suspend fun provideAvatarIcon(
            context: Context,
            data: PushData
    ): IconCompat? {
        return data.user.avatarURL?.let { url ->
            getBitmapFromUrl(url)?.let { IconCompat.createWithAdaptiveBitmap(it) }
        }
    }

    override suspend fun buildNotification(context: Context, data: PushData): Notification {
        val notificationId = data.channel.id.toInt()
        val messagingStyle = notificationManager.extractMessagingStyle(notificationId)
                ?: data.createMessagingStyle()
        messagingStyle.addMessage(data.toNotificationStyle())

        val notificationBuilder = NotificationCompat.Builder(context, provideNotificationChannelId())
            .setSmallIcon(provideNotificationSmallIcon())
            .setStyle(messagingStyle)
            .setContentIntent(providePendingIntent(context, data))
            .apply {
                provideActions(
                    notificationId = notificationId,
                    data = data
                ).forEach(::addAction)
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

    protected open suspend fun PushData.toNotificationStyle() = with(message) {
        MessagingStyle.Message(
            SceytChatUIKit.formatters.notificationBodyFormatter.format(context, this@toNotificationStyle),
            createdAt.takeIf { it != 0L } ?: System.currentTimeMillis(),
            getPerson(),
        )
    }

    protected open fun NotificationManagerCompat.extractMessagingStyle(
            notificationId: Int
    ): MessagingStyle? {
        val notification = activeNotifications.firstOrNull {
            it.id == notificationId
        }?.notification ?: return null
        return MessagingStyle.extractMessagingStyleFromNotification(notification)
    }

    protected open suspend fun PushData.createMessagingStyle() = MessagingStyle(getPerson())
        .setConversationTitle(channel.channelSubject)

    protected open suspend fun SceytUser.toPerson(pushData: PushData): Person {
        return Person.Builder()
            .setKey(id)
            .setName(SceytChatUIKit.formatters.userNameFormatter.format(context, this))
            .setIcon(provideAvatarIcon(context, pushData))
            .build()
    }

    protected open suspend fun PushData.getPerson() = user.let {
        personsMap.getOrPut(it.id) {
            it.toPerson(this)
        }
    }

    protected open val pendingIntentFlags: Int
        get() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
}
