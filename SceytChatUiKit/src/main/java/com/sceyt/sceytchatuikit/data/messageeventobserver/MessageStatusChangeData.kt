package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.User

data class MessageStatusChangeData(
        val channelId: Long?,
        val from: User?,
        val status: DeliveryStatus,
        val messageIds: MutableList<Long>
)