package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Reaction

data class ReactionUpdateEventData(
        val message: Message,
        val reaction: Reaction,
        val eventType: ReactionUpdateEventEnum
)

enum class ReactionUpdateEventEnum {
    ADD,
    REMOVE
}