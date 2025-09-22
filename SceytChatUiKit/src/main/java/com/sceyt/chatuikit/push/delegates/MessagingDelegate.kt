package com.sceyt.chatuikit.push.delegates

import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDataParser
import com.sceyt.chatuikit.push.service.PushService
import org.koin.core.component.inject

abstract class MessagingDelegate : SceytKoinComponent {
    protected val pushService by inject<PushService>()

    fun getDataFromPayload(payload: Map<String, String>): PushData? {
        val type = payload["type"]?.toIntOrNull()?.let {
            NotificationType.entries.getOrNull(it)
        } ?: return null
        val user = PushDataParser.getUser(payload) ?: return null
        val channel = PushDataParser.getChannel(payload)?.toSceytUiChannel() ?: return null
        val message = PushDataParser.getMessage(payload, channel.id, user)?.toSceytUiMessage()
                ?: return null
        val reaction = PushDataParser.getReaction(payload, message.id, user)
        return PushData(
            type = type,
            channel = channel.copy(lastMessage = message),
            message = message,
            user = user.toSceytUser(),
            reaction = reaction
        )
    }
}