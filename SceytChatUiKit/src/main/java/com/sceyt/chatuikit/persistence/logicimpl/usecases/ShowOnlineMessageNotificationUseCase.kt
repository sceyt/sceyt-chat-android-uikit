package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.push.PushData

class ShowOnlineMessageNotificationUseCase(
        private val context: Context
) {
    suspend operator fun invoke(channel: SceytChannel, message: SceytMessage) {
        if (shouldShowOnlineNotification(message, channel)) {
            SceytChatUIKit.notifications.pushNotification.pushNotificationHandler.showNotification(
                context = context,
                data = PushData(
                    channel = channel,
                    message = message,
                    user = message.user ?: return,
                    reaction = null
                ))
        }
    }

    private fun shouldShowOnlineNotification(
            message: SceytMessage,
            channel: SceytChannel,
    ): Boolean {
        if (!message.incoming || channel.muted) return false
        val config = SceytChatUIKit.config.notificationConfig
        val pushData by lazy {
            PushData(
                message = message,
                channel = channel,
                user = message.user ?: return@lazy null,
                reaction = null)
        }
        val isAppInForeground = context.isAppOnForeground()
        val isChannelOpen = ChannelsCache.currentChannelId == channel.id && isAppInForeground
        return isChannelOpen.not()
                && config.isPushEnabled
                && (config.suppressWhenAppIsInForeground.not() || !isAppInForeground)
                && config.shouldDisplayNotification(pushData ?: return false)
    }
}