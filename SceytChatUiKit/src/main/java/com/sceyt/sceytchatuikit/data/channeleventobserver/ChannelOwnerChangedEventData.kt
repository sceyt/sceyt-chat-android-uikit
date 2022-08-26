package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.member.Member

data class ChannelOwnerChangedEventData(
        val channel: Channel,
        var newOwner: Member,
        val oldOwner: Member
)