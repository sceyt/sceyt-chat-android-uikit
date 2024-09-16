package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chat.models.member.Member
import com.sceyt.chatuikit.data.models.channels.SceytChannel

data class ChannelOwnerChangedEventData(
        val channel: SceytChannel,
        var newOwner: Member,
        val oldOwner: Member
)