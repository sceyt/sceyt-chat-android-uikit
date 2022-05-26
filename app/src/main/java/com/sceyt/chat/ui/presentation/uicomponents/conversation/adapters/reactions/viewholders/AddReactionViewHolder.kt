package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytItemAddReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class AddReactionViewHolder(private val binding: SceytItemAddReactionBinding,
                            private val messageListeners: MessageClickListenersImpl) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ReactionItem) {
        binding.root.setOnClickListener {
            messageListeners.onAddReactionClick(it, (item as ReactionItem.AddItem).messageItem)
        }
    }
}