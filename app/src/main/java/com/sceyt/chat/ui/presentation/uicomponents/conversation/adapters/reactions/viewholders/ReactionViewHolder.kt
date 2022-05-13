package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ItemReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class ReactionViewHolder(private val binding: ItemReactionBinding,
                         private val messageListeners: MessageClickListenersImpl) : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: ReactionItem) {
        data as ReactionItem.Reaction
        binding.tvEmoji.text = data.reactionScore.key
        binding.tvCount.text = data.reactionScore.score.toString()

        binding.root.setOnLongClickListener {
            messageListeners.onReactionLongClick(it, data)
            showReactionPopup(it, data)
            return@setOnLongClickListener false
        }
    }

    private fun showReactionPopup(view: View, reaction: ReactionItem.Reaction) {
        val popup = PopupMenu(itemView.context, view)
        popup.menu.apply {
            add(0, R.id.add, 0, itemView.context.getString(R.string.add))
            add(0, R.id.remove, 0, itemView.context.getString(R.string.remove))
            add(0, R.id.delete, 0, itemView.context.getString(R.string.delete))
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add -> messageListeners.onAddReactionClick(reaction.messageItem, bindingAdapterPosition)
                //R.id.remove -> onReduceReactionCb(reaction)
                //   R.id.delete -> onDeleteReactionCb(reaction)
            }
            false
        }
        popup.show()
    }
}