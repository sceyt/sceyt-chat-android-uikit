package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytItemReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class ReactionViewHolder(private val binding: SceytItemReactionBinding,
                         private val messageListeners: MessageClickListenersImpl?) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var reactionItem: ReactionItem.Reaction

    init {
        binding.root.setOnClickListener {
            messageListeners?.onReactionClick(it, reactionItem)
        }
    }

    fun bind(data: ReactionItem) {
        if (data !is ReactionItem.Reaction) return
        reactionItem = data

        binding.reactionView.setCountAndSmile(data.reaction.score, data.reaction.key)
        if (data.reaction.containsSelf)
            binding.reactionView.setReactionBackgroundColor("#4BBEFD".toColorInt())
        else binding.reactionView.setReactionBackgroundColor(0)
    }
}