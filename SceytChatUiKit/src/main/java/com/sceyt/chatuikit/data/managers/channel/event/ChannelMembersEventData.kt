package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember

data class ChannelMembersEventData(
        val channel: SceytChannel,
        val members: List<SceytMember>,
        val eventType: ChannelMembersEventEnum
)