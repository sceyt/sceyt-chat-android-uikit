package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.sceytchatuikit.presentation.common.SelectableItem

sealed class ReactionHeaderItem : SelectableItem() {

    data class Reaction(
            var reactionTotal: ReactionTotal
    ) : ReactionHeaderItem()

    data class All(
            val count: Long
    ) : ReactionHeaderItem()
}