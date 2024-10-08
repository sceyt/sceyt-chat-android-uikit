package com.sceyt.chatuikit.data.managers.message.event

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser

data class MessageStatusChangeData(
        val channel: SceytChannel,
        val from: SceytUser,
        val status: DeliveryStatus,
        val marker: MessageListMarker
)