package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel

data class MessageStatusChangeData(
        val channel: SceytChannel,
        val from: User?,
        val status: DeliveryStatus,
        val messageIds: MutableList<Long>
)