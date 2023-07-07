package com.sceyt.sceytchatuikit.pushes

import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

data class RemoteMessageData(
        val channel: SceytChannel?,
        val message: SceytMessage?,
        val user: User?,
        val reactionTotal: ReactionTotal?
)