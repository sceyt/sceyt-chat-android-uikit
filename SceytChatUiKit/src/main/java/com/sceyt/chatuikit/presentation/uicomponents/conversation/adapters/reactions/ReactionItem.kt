package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.reactions

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.presentation.common.SelectableItem

sealed class ReactionItem : SelectableItem() {
    data class Reaction(val reaction: SceytReactionTotal,
                        val message: SceytMessage,
                        var isPending: Boolean) : ReactionItem()

    data class Other(val message: SceytMessage) : ReactionItem()
}