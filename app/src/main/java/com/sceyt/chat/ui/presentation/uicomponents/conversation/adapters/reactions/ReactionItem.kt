package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions

import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem

sealed class ReactionItem {
    data class Reaction(val reactionScore: ReactionScore,
                        val messageItem: MessageListItem.MessageItem) : ReactionItem() {
        override fun equals(other: Any?): Boolean {
            return super.equals(other)
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + reactionScore.hashCode()
            result = 31 * result + messageItem.hashCode()
            return result
        }
    }

    data class AddItem(val messageItem: MessageListItem.MessageItem) : ReactionItem(){
        override fun equals(other: Any?): Boolean {
            return super.equals(other)
        }
    }

    override fun equals(other: Any?): Boolean {
        val value= when {
            other == null -> false
            other !is ReactionItem -> false
            other is Reaction && this is Reaction -> {
                other.reactionScore.key == reactionScore.key &&
                        other.reactionScore.score == reactionScore.score
            }
            other is AddItem && this is AddItem -> true
            else -> false
        }

        return value
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}