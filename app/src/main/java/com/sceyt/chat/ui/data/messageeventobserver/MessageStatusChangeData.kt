package com.sceyt.chat.ui.data.messageeventobserver

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.User

data class MessageStatusChangeData(
        val channel: Channel?,
        val from: User?,
        val status: DeliveryStatus,
        val messageIds: MutableList<Long>
)