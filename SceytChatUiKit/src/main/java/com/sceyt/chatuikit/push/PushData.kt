package com.sceyt.chatuikit.push

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytUser

data class PushData(
        val channel: SceytChannel,
        val message: SceytMessage,
        val user: SceytUser,
        val reaction: SceytReaction?
)