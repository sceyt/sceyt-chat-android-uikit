package com.sceyt.chatuikit.pushes

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction

data class RemoteMessageData(
        val channel: SceytChannel?,
        val message: SceytMessage?,
        val user: User?,
        val reaction: SceytReaction?
)