package com.sceyt.chatuikit.push.delegates

import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.getChannelFromPush
import com.sceyt.chatuikit.push.getMessageFromPush
import com.sceyt.chatuikit.push.getReactionFromPush
import com.sceyt.chatuikit.push.getUserFromPush
import com.sceyt.chatuikit.push.service.PushService
import org.koin.core.component.inject

open class MessagingDelegate : SceytKoinComponent {
    protected val pushService by inject<PushService>()

    fun getDataFromPayload(payload: Map<String, String>): PushData? {
        val type = payload["type"]?.toIntOrNull()?.let {
            NotificationType.entries.getOrNull(it)
        } ?: return null
        val user = getUserFromPush(payload) ?: return null
        val channel = getChannelFromPush(payload)?.toSceytUiChannel() ?: return null
        val message = getMessageFromPush(payload, channel.id, user)?.toSceytUiMessage()
                ?: return null
        val reaction = getReactionFromPush(payload, message.id, user)
        return PushData(type, channel, message, user.toSceytUser(), reaction)
    }
}