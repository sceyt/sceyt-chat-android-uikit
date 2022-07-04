package com.sceyt.chat.ui.data.channeleventobserverservice

import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember

data class ChannelTypingEventData(
        val channel: SceytChannel,
        val member: SceytMember,
        val typing: Boolean
)