package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytUiItemAddReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class AddReactionViewHolder(private val binding: SceytUiItemAddReactionBinding,
                            private val messageListeners: MessageClickListenersImpl) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ReactionItem) {
        binding.root.setOnClickListener {
            messageListeners.onAddReactionClick((item as ReactionItem.AddItem).messageItem, bindingAdapterPosition)
        }
    }
}