package com.sceyt.chatuikit.notifications.managers

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.SceytChatUIKit.notifications
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventData
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.notifications.NotificationType
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

    override suspend fun onMessageReceived(channel: SceytChannel, message: SceytMessage) {
        // Check maybe event comes from carbon user, if true then ignore
        if (message.user?.id == SceytChatUIKit.currentUserId) return
        val pushData = PushData(
            type = NotificationType.ChannelMessage,
            channel = channel,
            message = message,
            user = message.user ?: return,
            reaction = null
        )
        showNotificationIfNeeded(pushData)
    }

    override suspend fun onMessageStateChanged(message: SceytMessage) {
        notifications.pushNotification.notificationHandler.onMessageStateChanged(
            context = context,
            message = message
        )
    }

    override suspend fun onReactionEvent(data: ReactionUpdateEventData) {
        val channelId = data.message.channelId
        // If message is incoming ignore
        if (data.message.incoming) return
        // Check maybe event comes from carbon user, if true then ignore
        if (data.reaction.user?.id == SceytChatUIKit.currentUserId) return

        when (data.eventType) {
            ReactionUpdateEventEnum.Add -> {
                val channel = SceytChatUIKit.chatUIFacade.channelInteractor.getChannelFromDb(channelId)
                        ?: return

                val pushData = PushData(
                    type = NotificationType.MessageReaction,
                    channel = channel,
                    message = data.message,
                    user = data.reaction.user ?: return,
                    reaction = data.reaction
                )

                showNotificationIfNeeded(pushData)
            }

            ReactionUpdateEventEnum.Remove -> {
                notifications.pushNotification.notificationHandler.onReactionDeleted(
                    context = context,
                    message = data.message,
                    reaction = data.reaction
                )
            }
        }
    }

    private suspend fun showNotificationIfNeeded(pushData: PushData) {
        if (showNotificationUseCase(pushData)) {
            notifications.pushNotification.notificationHandler.showNotification(
                context = context,
                data = pushData
            )
        }
    }
}