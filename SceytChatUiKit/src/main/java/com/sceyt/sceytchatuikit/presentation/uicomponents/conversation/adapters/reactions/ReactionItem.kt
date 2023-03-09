package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions

import com.sceyt.sceytchatuikit.data.models.messages.ReactionData
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.common.SelectableItem

sealed class ReactionItem : SelectableItem() {
    data class Reaction(val reaction: ReactionData,
                        val message: SceytMessage) : ReactionItem()

    data class Other(val message: SceytMessage) : ReactionItem()
}