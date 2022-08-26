package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember


data class ChannelTypingEventData(
        val channel: SceytChannel,
        val member: SceytMember,
        val typing: Boolean
)