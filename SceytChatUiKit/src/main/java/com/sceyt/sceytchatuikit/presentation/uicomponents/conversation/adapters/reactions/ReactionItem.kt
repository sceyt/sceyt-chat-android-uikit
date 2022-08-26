package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction

sealed class ReactionItem {
    data class Reaction(val reaction: SceytReaction,
                        val message: SceytMessage) : ReactionItem()

    data class AddItem(val message: SceytMessage) : ReactionItem()
}