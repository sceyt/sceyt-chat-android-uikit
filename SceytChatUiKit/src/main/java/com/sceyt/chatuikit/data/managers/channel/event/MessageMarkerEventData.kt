package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytChannel

data class MessageMarkerEventData(
        val channel: SceytChannel,
        val user: User,
        val marker: MessageListMarker
)
