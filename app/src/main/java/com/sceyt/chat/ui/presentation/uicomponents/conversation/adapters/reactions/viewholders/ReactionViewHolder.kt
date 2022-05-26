package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytItemReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class ReactionViewHolder(private val binding: SceytItemReactionBinding,
                         private val messageListeners: MessageClickListenersImpl) : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: ReactionItem) {
        data as ReactionItem.Reaction
        binding.reactionView.setCountAndSmile(data.reactionScore.score, data.reactionScore.key)

        binding.root.setOnLongClickListener {
            messageListeners.onReactionLongClick(it, data)
            return@setOnLongClickListener false
        }
    }
}