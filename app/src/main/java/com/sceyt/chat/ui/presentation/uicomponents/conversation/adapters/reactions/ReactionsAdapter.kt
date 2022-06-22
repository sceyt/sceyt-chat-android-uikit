package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.AddReactionViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolderFactory

class ReactionsAdapter(
        private val viewHolderFactory: ReactionViewHolderFactory
) : ListAdapter<ReactionItem, RecyclerView.ViewHolder>(REACTIONS_DIFF_UTIL) {

    companion object {
        val REACTIONS_DIFF_UTIL = object : DiffUtil.ItemCallback<ReactionItem>() {
            override fun areItemsTheSame(oldItem: ReactionItem, newItem: ReactionItem): Boolean {
                if (oldItem is ReactionItem.AddItem) return true
                if (newItem is ReactionItem.AddItem) return true
                return ((oldItem as ReactionItem.Reaction).reaction.key == (newItem as ReactionItem.Reaction).reaction.key)
            }

            override fun areContentsTheSame(oldItem: ReactionItem, newItem: ReactionItem): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: ReactionItem, newItem: ReactionItem): Any {
                return newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ReactionViewHolderFactory.ReactionViewType.Reaction.ordinal -> {
                (holder as ReactionViewHolder).bind(currentList[position])
            }
            ReactionViewHolderFactory.ReactionViewType.Add.ordinal -> {
                (holder as AddReactionViewHolder).bind(ReactionItem.AddItem(
                    ((currentList[0] as? ReactionItem.Reaction))?.message ?: return))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(position, currentList.size)
    }

    override fun getItemCount(): Int {
        return currentList.size + 1
    }
}