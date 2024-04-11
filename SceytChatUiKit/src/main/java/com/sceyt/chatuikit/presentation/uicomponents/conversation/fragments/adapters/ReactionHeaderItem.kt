package com.sceyt.chatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.presentation.common.SelectableItem

sealed class ReactionHeaderItem : SelectableItem() {

    data class Reaction(
            var reactionTotal: SceytReactionTotal
    ) : ReactionHeaderItem()

    data class All(
            val count: Long
    ) : ReactionHeaderItem()
}