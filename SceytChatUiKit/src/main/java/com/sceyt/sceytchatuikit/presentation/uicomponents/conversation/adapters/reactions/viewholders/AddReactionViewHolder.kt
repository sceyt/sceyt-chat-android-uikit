package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.databinding.SceytItemAddReactionBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class AddReactionViewHolder(private val binding: SceytItemAddReactionBinding,
                            private val messageListeners: MessageClickListenersImpl?) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ReactionItem) {
        binding.root.setOnClickListener {
            messageListeners?.onAddReactionClick(it, (item as ReactionItem.AddItem).message)
        }
    }
}