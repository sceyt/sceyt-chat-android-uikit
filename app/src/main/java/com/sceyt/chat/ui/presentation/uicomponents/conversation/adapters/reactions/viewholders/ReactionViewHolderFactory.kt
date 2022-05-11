package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.ItemAddReactionBinding
import com.sceyt.chat.ui.databinding.ItemReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

class ReactionViewHolderFactory(context: Context) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ReactionViewType.Default.ordinal -> {
                ReactionViewHolder(ItemReactionBinding.inflate(layoutInflater, parent, false))
            }
            ReactionViewType.Add.ordinal -> {
                AddReactionViewHolder(ItemAddReactionBinding.inflate(layoutInflater, parent, false))
            }
            else -> throw Exception("Not supported view type")
        }
    }

    fun getItemViewType(item: ReactionItem): Int {
        return when (item) {
            is ReactionItem.Reaction -> ReactionViewType.Default.ordinal
            is ReactionItem.AddItem -> ReactionViewType.Add.ordinal
        }
    }

    enum class ReactionViewType {
        Add, Default
    }
}