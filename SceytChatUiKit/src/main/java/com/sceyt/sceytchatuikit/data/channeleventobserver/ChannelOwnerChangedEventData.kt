package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.chat.models.member.Member
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel

data class ChannelOwnerChangedEventData(
        val channel: SceytChannel,
        var newOwner: Member,
        val oldOwner: Member
)