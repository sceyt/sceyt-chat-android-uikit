package com.sceyt.chat.ui.presentation.uicomponents.conversation.events

import com.sceyt.chat.ui.data.models.messages.SceytMessage

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