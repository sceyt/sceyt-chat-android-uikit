package com.sceyt.chat.ui.presentation.uicomponents.conversation.events

import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage

sealed class ReactionEvent {

    data class AddReaction(
            val message: SceytUiMessage,
            val score: ReactionScore
    ) : ReactionEvent()

    data class DeleteReaction(
            val message: SceytUiMessage,
            val score: ReactionScore
    ) : ReactionEvent()
}