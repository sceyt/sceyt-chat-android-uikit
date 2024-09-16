package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.holders.ReactionViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.holders.ReactionViewHolderFactory

class ReactionsAdapter(
        private val viewHolderFactory: ReactionViewHolderFactory,
        private val message: SceytMessage
) : ListAdapter<ReactionItem.Reaction, RecyclerView.ViewHolder>(REACTIONS_DIFF_UTIL) {

    companion object {
        val REACTIONS_DIFF_UTIL = object : DiffUtil.ItemCallback<ReactionItem.Reaction>() {
            override fun areItemsTheSame(oldItem: ReactionItem.Reaction, newItem: ReactionItem.Reaction): Boolean {
                return oldItem.reaction.key == newItem.reaction.key
            }

            override fun areContentsTheSame(oldItem: ReactionItem.Reaction, newItem: ReactionItem.Reaction): Boolean {
                return oldItem.reaction.key == newItem.reaction.key
            }

            override fun getChangePayload(oldItem: ReactionItem.Reaction, newItem: ReactionItem.Reaction): Any {
                return Any()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderFactory.createViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val shouldShowCount = (currentList.size > 1 || (currentList.getOrNull(0)?.reaction?.score
                ?: 0) > 1) && position == currentList.size - 1
        (holder as ReactionViewHolder).bind(currentList[position], shouldShowCount, message)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}