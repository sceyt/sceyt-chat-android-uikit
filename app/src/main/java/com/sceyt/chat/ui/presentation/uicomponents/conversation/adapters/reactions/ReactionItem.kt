package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions

import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.models.messages.SceytReaction

sealed class ReactionItem {
    data class Reaction(val reaction: SceytReaction,
                        val message: SceytMessage) : ReactionItem()

    data class AddItem(val message: SceytMessage) : ReactionItem()
}