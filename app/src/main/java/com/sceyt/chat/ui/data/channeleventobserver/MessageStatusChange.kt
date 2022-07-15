package com.sceyt.chat.ui.data.channeleventobserver

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.User

data class MessageStatusChange(
        val channel: Channel?,
        val from: User?,
        val status: DeliveryStatus,
        val messageIds: MutableList<Long>
)