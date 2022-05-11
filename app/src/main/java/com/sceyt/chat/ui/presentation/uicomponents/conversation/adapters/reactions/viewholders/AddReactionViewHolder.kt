package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.ItemAddReactionBinding

class AddReactionViewHolder(private val binding: ItemAddReactionBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind() {
        binding.root.setOnClickListener {
            //  onAddNewReactionCb.invoke()
        }
    }
}