package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.sceytchatuikit.presentation.common.SelectableItem

sealed class ReactionHeaderItem : SelectableItem() {

    data class Reaction(
            var reactionScore: ReactionScore
    ) : ReactionHeaderItem()

    data class All(
            val count: Long
    ) : ReactionHeaderItem()
}