package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.sceytchatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.sceytchatuikit.presentation.common.SelectableItem

sealed class ReactionHeaderItem : SelectableItem() {

    data class Reaction(
            var reactionTotal: SceytReactionTotal
    ) : ReactionHeaderItem()

    data class All(
            val count: Long
    ) : ReactionHeaderItem()
}