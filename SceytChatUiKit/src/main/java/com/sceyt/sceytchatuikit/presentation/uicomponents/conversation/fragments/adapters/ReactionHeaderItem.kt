package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.sceytchatuikit.data.models.messages.ReactionData
import com.sceyt.sceytchatuikit.presentation.common.SelectableItem

sealed class ReactionHeaderItem : SelectableItem() {

    data class Reaction(
            var reactionTotal: ReactionData
    ) : ReactionHeaderItem()

    data class All(
            val count: Long
    ) : ReactionHeaderItem()
}