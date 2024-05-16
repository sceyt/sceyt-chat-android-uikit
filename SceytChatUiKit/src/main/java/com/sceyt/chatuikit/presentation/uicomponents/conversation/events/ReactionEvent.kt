package com.sceyt.chatuikit.presentation.uicomponents.conversation.events

import com.sceyt.chatuikit.data.models.messages.SceytMessage

sealed class ReactionEvent {

    data class AddReaction(
            val message: SceytMessage,
            val scoreKey: String
    ) : ReactionEvent()

    data class RemoveReaction(
            val message: SceytMessage,
            val scoreKey: String,
            val isPending: Boolean
    ) : ReactionEvent()
}