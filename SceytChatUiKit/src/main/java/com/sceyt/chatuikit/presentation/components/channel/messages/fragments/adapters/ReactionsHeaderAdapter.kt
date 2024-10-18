package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.ReactionTotal
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class ReactionsHeaderAdapter(
        data: List<ReactionHeaderItem>,
        private val factory: HeaderViewHolderFactory,
) : RecyclerView.Adapter<BaseViewHolder<ReactionHeaderItem>>() {
    private val data = data.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReactionHeaderItem> {
        return factory.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReactionHeaderItem>, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return factory.getItemViewType(data[position])
    }

    fun setSelected(position: Int) {
        if (data.isEmpty() || position !in (0 until data.size)) return
        data.findIndexed { it.selected }?.let {
            it.second.selected = false
            notifyItemChanged(it.first, Any())
        }
        data[position].selected = true
        notifyItemChanged(position, Any())
    }

    fun addOrUpdateItem(reaction: ReactionTotal) {
        data.findIndexed { it is ReactionHeaderItem.Reaction && it.reactionTotal.key == reaction.key }?.let {
            (it.second as ReactionHeaderItem.Reaction).reactionTotal = SceytReactionTotal(reaction.key, reaction.score.toInt(), false)
            notifyItemChanged(it.first, Any())
        } ?: let {
            data.add(ReactionHeaderItem.Reaction(SceytReactionTotal(reaction.key, reaction.score.toInt(), false)))
            notifyItemInserted(data.lastIndex)
        }
    }

    fun removeItem(reaction: SceytReaction) {
        data.findIndexed { it is ReactionHeaderItem.Reaction && it.reactionTotal.key == reaction.key }?.let {
            data.removeAt(it.first)
            notifyItemRemoved(it.first)
        }
    }

    fun updateAppItem(sumOf: Long) {
        if (data.isEmpty()) return
        data[0] = ReactionHeaderItem.All(sumOf).also { it.selected = data[0].selected }
        notifyItemChanged(0, Any())
    }
}