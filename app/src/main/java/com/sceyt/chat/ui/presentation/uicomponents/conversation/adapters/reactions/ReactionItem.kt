package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions

import com.sceyt.chat.models.message.ReactionScore

sealed class ReactionItem {
    data class Reaction(val reactionScore: ReactionScore) : ReactionItem()
    object AddItem : ReactionItem()

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other !is ReactionItem -> false
            other is Reaction && this is Reaction -> {
                other.reactionScore.key == reactionScore.key &&
                        other.reactionScore.score == reactionScore.score
            }
            other is AddItem && this is AddItem -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}