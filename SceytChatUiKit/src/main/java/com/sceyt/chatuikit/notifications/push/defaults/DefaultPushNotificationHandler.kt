package com.sceyt.chatuikit.notifications.push.defaults

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.getPerson
import com.sceyt.chatuikit.notifications.builder.NotificationBuilderHelper.toMessagingStyle
import com.sceyt.chatuikit.notifications.extractMessagingStyle
import com.sceyt.chatuikit.notifications.push.PushNotificationHandler
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder.Companion.EXTRAS_CHAT_NOTIFICATION
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder.Companion.EXTRAS_MESSAGE_ID
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder.Companion.EXTRAS_REACTION_ID
import com.sceyt.chatuikit.push.PushData
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

open class DefaultPushNotificationHandler(
        private val context: Context
) : PushNotificationHandler {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val notificationManager by lazy { NotificationManagerCompat.from(context) }
    private val mutex by lazy { Mutex() }
    private val notificationBuilder by lazy { notifications.pushNotification.notificationBuilder }
    private val showedNotifications = Collections.synchronizedSet(mutableSetOf<Long>())

    override suspend fun showNotification(
            context: Context,
            data: PushData,
    ) {
        if (checkSelfPermission(context, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return

        if (checkMaybeAlreadyShown(data)) {
            SceytLog.i(TAG, "Notification already shown. ${data.message.body}")
            return
        }

        val notificationId = data.channel.id.toInt()
        val notification = notifications.pushNotification.notificationBuilder.buildNotification(
            context = context,
            data = data,
            notificationId = notificationId
        )
        notificationManager.notify(notificationId, notification)
    }

    override suspend fun onMessageStateChanged(context: Context, message: SceytMessage) {
        if (checkSelfPermission(context, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return

        if (!SceytChatUIKit.config.notificationConfig.isPushEnabled)
            return

        mutex.withLock {
            val notificationId = message.channelId.toInt()
            notificationManager.activeNotifications.firstOrNull {
                it.id == notificationId && it.notification.extras.getBoolean(EXTRAS_CHAT_NOTIFICATION)
            } ?: return@withLock

            val style = notificationManager.extractMessagingStyle(notificationId) ?: return@withLock
            val channel = SceytChatUIKit.chatUIFacade.channelInteractor.getChannelFromDb(
                channelId = message.channelId
            ) ?: return@withLock

            val pushData = PushData(
                type = NotificationType.ChannelMessage,
                channel = channel,
                message = message,
                user = message.user ?: return@withLock,
                reaction = null
            )

            val messages = style.messages
            when (message.state) {
                MessageState.Deleted, MessageState.DeletedHard -> {
                    onNotificationMessageDeleted(
                        notificationId = notificationId,
                        dataProvider = { pushData },
                        predicate = { it.extras.getLong(EXTRAS_MESSAGE_ID) == message.id }
                    )
                }

                else -> {
                    val person = NotificationBuilderHelper.personMap[pushData.user.id]
                            ?: pushData.getPerson(
                                context = context,
                                icon = notificationBuilder.provideAvatarIcon(context, pushData)
                            )
                    messages.indexOfFirst {
                        it.extras.getLong(EXTRAS_MESSAGE_ID) == message.id
                    }.takeIf { it != -1 }?.let {
                        messages[it] = pushData.toMessagingStyle(context, person)
                    } ?: return@withLock

                    notificationBuilder.buildNotification(
                        context = context,
                        data = pushData,
                        notificationId = notificationId,
                        style = style,
                        builderCustomizer = { setSilent(true) }
                    ).let {
                        notificationManager.notify(notificationId, it)
                    }
                }
            }

            // Delay for a short period to ensure the notification is removed before re-creating it
            delay(50)
        }
    }

    override suspend fun onReactionDeleted(
            context: Context,
            message: SceytMessage,
            reaction: SceytReaction
    ) {
        if (checkSelfPermission(context, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return
        val user = reaction.user ?: return

        mutex.withLock {
            val notificationId = message.channelId.toInt()
            val channel = SceytChatUIKit.chatUIFacade.channelInteractor.getChannelFromDb(message.channelId)
                    ?: return@withLock

            onNotificationMessageDeleted(
                notificationId = notificationId,
                dataProvider = {
                    PushData(
                        type = NotificationType.MessageReaction,
                        channel = channel,
                        message = message,
                        user = user,
                        reaction = reaction
                    )
                },
                predicate = {
                    it.extras.getBoolean(EXTRAS_CHAT_NOTIFICATION) &&
                            it.extras.getLong(EXTRAS_REACTION_ID) == reaction.id
                }
            )
            // Delay for a short period to ensure the notification is removed before re-creating it
            delay(50)
        }
    }

    override fun cancelNotification(notificationId: Int) {
        notificationManager.activeNotifications.firstOrNull {
            it.id == notificationId && it.notification.extras.getBoolean(EXTRAS_CHAT_NOTIFICATION)
        } ?: return

        notificationManager.cancel(notificationId)
    }

    override fun cancelAllNotifications() {
        notificationManager.activeNotifications.forEach {
            if (it.notification.extras.getBoolean(EXTRAS_CHAT_NOTIFICATION))
                notificationManager.cancel(it.id)
        }
    }

    @RequiresPermission(POST_NOTIFICATIONS)
    protected open suspend fun onNotificationMessageDeleted(
            notificationId: Int,
            dataProvider: () -> PushData,
            predicate: (NotificationCompat.MessagingStyle.Message) -> Boolean
    ) {
        val style = notificationManager.extractMessagingStyle(notificationId) ?: return
        val messages = style.messages
        messages.indexOfFirst(predicate).takeIf { it != -1 }?.let {
            messages.removeAt(it)
            if (messages.isEmpty()) {
                notificationManager.cancel(notificationId)
                return
            }
        } ?: return

        notificationBuilder.buildNotification(
            context = context,
            data = dataProvider(),
            notificationId = notificationId,
            style = style,
            builderCustomizer = { setSilent(true) }
        ).let {
            notificationManager.notify(notificationId, it)
        }
    }

    protected open fun checkMaybeAlreadyShown(pushData: PushData): Boolean {
        return when (pushData.type) {
            NotificationType.ChannelMessage -> {
                if (showedNotifications.contains(pushData.message.tid)) {
                    return true
                }
                showedNotifications.add(pushData.message.tid)
                false
            }

            NotificationType.MessageReaction -> {
                if (showedNotifications.contains(pushData.reaction?.id ?: return false)) {
                    return true
                }
                showedNotifications.add(pushData.reaction.id)
                false
            }
        }
    }
}