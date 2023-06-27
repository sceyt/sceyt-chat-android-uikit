package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.databinding.SceytItemReactionBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners

class ReactionViewHolder(private val binding: SceytItemReactionBinding,
                         private val messageListeners: MessageClickListeners.ClickListeners?) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var reactionItem: ReactionItem.Reaction

    init {
        binding.root.setOnClickListener {
            messageListeners?.onReactionClick(it, reactionItem)
        }
    }

    fun bind(data: ReactionItem, shouldShowCount: Boolean) {
        if (data !is ReactionItem.Reaction) return
        reactionItem = data
        if (shouldShowCount) {
            val count = data.message.reactionTotals?.sumOf { it.score } ?: 0
            binding.reactionView.setCountAndSmile(count, data.reaction.key)
        } else
            binding.reactionView.setSmileText(data.reaction.key, true)
    }
}