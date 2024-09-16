package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember


data class ChannelTypingEventData(
        val channel: SceytChannel,
        val member: SceytMember,
        val typing: Boolean
)