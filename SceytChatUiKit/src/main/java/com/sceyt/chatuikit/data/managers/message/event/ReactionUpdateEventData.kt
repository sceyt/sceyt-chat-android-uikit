package com.sceyt.chatuikit.data.managers.message.event

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction

data class ReactionUpdateEventData(
        val message: SceytMessage,
        val reaction: SceytReaction,
        val eventType: ReactionUpdateEventEnum
)

enum class ReactionUpdateEventEnum {
    Add,
    Remove
}