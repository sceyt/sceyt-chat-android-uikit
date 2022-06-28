package com.sceyt.chat.ui.data.channeleventobserverservice

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.member.Member

data class ChannelTypingEventData(
        val channel: Channel?,
        val member: Member?,
        val typing: Boolean
)