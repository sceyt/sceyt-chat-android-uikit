package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

data class ReactionUpdateEventData(
        val message: SceytMessage,
        val reaction: Reaction,
        val eventType: ReactionUpdateEventEnum
)

enum class ReactionUpdateEventEnum {
    ADD,
    REMOVE
}