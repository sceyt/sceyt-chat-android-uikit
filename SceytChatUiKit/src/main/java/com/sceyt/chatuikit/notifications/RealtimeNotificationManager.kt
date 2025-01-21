package com.sceyt.chatuikit.notifications

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventData
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.notifications.push.defaults.DefaultPushNotificationBuilder.Companion.EXTRAS_MESSAGE_ID
import com.sceyt.chatuikit.persistence.logicimpl.usecases.ShouldShowNotificationUseCase
import com.sceyt.chatuikit.push.PushData

internal interface RealtimeNotificationManager {
    suspend fun onMessageReceived(channel: SceytChannel, message: SceytMessage)
    suspend fun onMessageStateChanged(message: SceytMessage)
    suspend fun onReactionEvent(data: ReactionUpdateEventData)
}

internal class RealtimeNotificationManagerImpl(
        private val context: Context,
        private val showNotificationUseCase: ShouldShowNotificationUseCase,
) : RealtimeNotificationManager {
    private val notificationManager by lazy {
        NotificationManagerCompat.from(context)
    }

    override suspend fun onMessageReceived(channel: SceytChannel, message: SceytMessage) {
        val pushData = PushData(
            type = NotificationType.ChannelMessage,
            channel = channel,
            message = message,
            user = message.user ?: return,
            reaction = null
        )
        if (showNotificationUseCase(pushData)) {
            SceytChatUIKit.notifications.pushNotification.pushNotificationHandler.showNotification(
                context = context,
                data = pushData,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun onMessageStateChanged(message: SceytMessage) {
        if (message.state == MessageState.Deleted) {

            notificationManager.activeNotifications.firstOrNull {
                it.id == message.channelId.toInt()
            } ?: return

            val style = notificationManager.extractMessagingStyle(message.channelId.toInt())
                    ?: return

            val messages = style.messages.filter {
                it.extras.getLong(EXTRAS_MESSAGE_ID) != message.id
            }

            val channel = SceytChatUIKit.chatUIFacade.channelInteractor.getChannelFromDb(message.channelId)
                    ?: return


            val pushData = PushData(
                type = NotificationType.ChannelMessage,
                channel = channel,
                message = message,
                user = message.user ?: return,
                reaction = null

            )
            style.messages.clear()
            style.messages.addAll(messages)


           /* val newn = SceytChatUIKit.notifications.pushNotification.notificationBuilder.buildNotification(
                context = context,
                data = pushData,
                notificationId = message.channelId.toInt()
            ).setStyle(style).build()

            notificationManager.notify(message.channelId.toInt(), newn)*/
        }
    }

    override suspend fun onReactionEvent(data: ReactionUpdateEventData) {
        handleReactionEvent(data)
    }

    private suspend fun handleReactionEvent(data: ReactionUpdateEventData) {
        val channelId = data.message.channelId
        if (!data.message.incoming && data.eventType == ReactionUpdateEventEnum.Add) {
            val channel = SceytChatUIKit.chatUIFacade.channelInteractor.getChannelFromDb(channelId)
                    ?: return

            val message = data.message
            val pushData = PushData(
                type = NotificationType.MessageReaction,
                channel = channel,
                message = message,
                user = data.reaction.user ?: return,
                reaction = data.reaction
            )

            if (showNotificationUseCase(pushData)) {
                SceytChatUIKit.notifications.pushNotification.pushNotificationHandler.showNotification(
                    context = context,
                    data = pushData
                )
            }
        }
    }
}