package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction

data class ReactionUpdateEventData(
        val message: SceytMessage,
        val reaction: SceytReaction,
        val eventType: ReactionUpdateEventEnum
)

enum class ReactionUpdateEventEnum {
    Add,
    Remove
}