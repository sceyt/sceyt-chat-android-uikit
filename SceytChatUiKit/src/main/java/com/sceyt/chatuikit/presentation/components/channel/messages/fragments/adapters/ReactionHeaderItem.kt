package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

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