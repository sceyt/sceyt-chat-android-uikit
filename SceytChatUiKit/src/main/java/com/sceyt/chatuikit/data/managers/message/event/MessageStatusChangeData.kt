package com.sceyt.chatuikit.data.managers.message.event

import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser

data class MessageStatusChangeData(
        val channel: SceytChannel,
        val from: SceytUser,
        val status: MessageDeliveryStatus,
        val marker: MessageListMarker
)