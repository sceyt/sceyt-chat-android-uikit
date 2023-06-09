package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

data class ChannelMembersEventData(
        val channel: SceytChannel,
        val members: List<SceytMember>,
        val eventType: ChannelMembersEventEnum
)