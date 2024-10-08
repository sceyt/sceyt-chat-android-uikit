package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.holders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemReactionBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners

class ReactionViewHolder(
        private val binding: SceytItemReactionBinding,
        private val onReactionClickListener: MessageClickListeners.ReactionClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: ReactionItem, shouldShowCount: Boolean, message: SceytMessage) {
        if (data !is ReactionItem.Reaction) return
        if (shouldShowCount) {
            val count = message.messageReactions?.sumOf { it.reaction.score } ?: 0
            binding.reactionView.setCountAndSmile(count, data.reaction.key)
        } else
            binding.reactionView.setSmileText(data.reaction.key, true)

        binding.root.setOnClickListener {
            onReactionClickListener?.onReactionClick(it, data, message)
        }
    }
}