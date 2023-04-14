package com.sceyt.sceytchatuikit.pushes

import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

data class RemoteMessageData(
        val channel: SceytChannel?,
        val message: SceytMessage?,
        val user: User?,
        val reactionScore: ReactionScore?
)