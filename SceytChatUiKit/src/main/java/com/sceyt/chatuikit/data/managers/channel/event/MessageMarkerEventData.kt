package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser

data class MessageMarkerEventData(
        val channel: SceytChannel,
        val user: SceytUser,
        val marker: MessageListMarker
)
