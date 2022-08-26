package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed class ReactionEvent {

    data class AddReaction(
            val message: SceytMessage,
            val scoreKey: String
    ) : ReactionEvent()

    data class DeleteReaction(
            val message: SceytMessage,
            val scoreKey: String
    ) : ReactionEvent()
}