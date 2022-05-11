package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.ItemReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

class ReactionViewHolder(private val binding: ItemReactionBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: ReactionItem) {
        data as ReactionItem.Reaction
        binding.tvEmoji.text = data.reactionScore.key
        binding.tvCount.text = data.reactionScore.score.toString()

        binding.root.setOnLongClickListener {
            showReactionPopup(it, data)
            return@setOnLongClickListener false
        }
    }

    private fun showReactionPopup(view: View, reaction: ReactionItem) {
        /*val popup = PopupMenu(itemView.context, view)
        popup.menu.apply {
            add(0, R.id.add, 0, itemView.context.getString(R.string.add))
            add(0, R.id.remove, 0, itemView.context.getString(R.string.remove))
            add(0, R.id.delete, 0, itemView.context.getString(R.string.delete))
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add -> onAddReactionCb(reaction)
                R.id.remove -> onReduceReactionCb(reaction)
                R.id.delete -> onDeleteReactionCb(reaction)
            }
            false
        }
        popup.show()*/
    }
}