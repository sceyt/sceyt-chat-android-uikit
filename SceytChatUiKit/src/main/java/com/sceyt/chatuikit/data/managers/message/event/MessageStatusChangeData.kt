package com.sceyt.chatuikit.data.managers.message.event

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytChannel

data class MessageStatusChangeData(
        val channel: SceytChannel,
        val from: User,
        val status: DeliveryStatus,
        val marker: MessageListMarker
)