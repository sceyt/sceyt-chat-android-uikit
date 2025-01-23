package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.push.PushData

class ShouldShowNotificationUseCase(
        private val context: Context
) {

    operator fun invoke(
            type: NotificationType,
            channel: SceytChannel,
            message: SceytMessage,
            reaction: SceytReaction?
    ): Boolean {
        val pushData = PushData(
            type = type,
            message = message,
            channel = channel,
            user = message.user ?: return false,
            reaction = reaction
        )
        return shouldShowOnlineNotification(pushData)
    }

    operator fun invoke(
            pushData: PushData
    ) = shouldShowOnlineNotification(pushData)

    private fun shouldShowOnlineNotification(
            pushData: PushData
    ): Boolean {
        val message = pushData.message
        val channel = pushData.channel

        // Check if channel is muted
        if (channel.muted)
            return false

        // Check maybe channel was cleared
        if (message.createdAt <= channel.messagesClearedAt || message.id <= channel.messagesClearedAt)
            return false

        if (!pushData.message.incoming && pushData.type != NotificationType.MessageReaction)
            return false

        val config = SceytChatUIKit.config.notificationConfig
        val isAppInForeground = context.isAppOnForeground()
        val isChannelOpen = ChannelsCache.currentChannelId == pushData.channel.id && isAppInForeground
        return isChannelOpen.not()
                && config.isPushEnabled
                && (config.suppressWhenAppIsInForeground.not() || !isAppInForeground)
                && config.shouldDisplayNotification(pushData)
    }
}