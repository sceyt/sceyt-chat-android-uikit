package com.sceyt.chat.ui.data.channeleventobserverservice

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.member.Member

data class ChannelMembersEventData(
        val channel: Channel?,
        val members: List<Member>?,
        val eventType: ChannelMembersEventEnum
)