package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.AddReactionViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolderFactory
import com.sceyt.chat.ui.utils.MyDiffUtil

class ReactionsAdapter(
        private val reactions: ArrayList<ReactionItem>,
        private val rvReactions: RecyclerView,
        private val viewHolderFactory: ReactionViewHolderFactory
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ReactionViewHolderFactory.ReactionViewType.Default.ordinal -> {
                (holder as ReactionViewHolder).bind(reactions[position])
            }
            ReactionViewHolderFactory.ReactionViewType.Add.ordinal -> {
                (holder as AddReactionViewHolder).bind(reactions[position])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(reactions[position])
    }

    override fun getItemCount(): Int {
        return reactions.size
    }

    fun submitData(list: List<ReactionItem>) {
        val myDiffUtil = MyDiffUtil(reactions, list)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        reactions.clear()
        reactions.addAll(list)
        productDiffResult.dispatchUpdatesTo(this)
    }

    val recyclerView get() = rvReactions
}