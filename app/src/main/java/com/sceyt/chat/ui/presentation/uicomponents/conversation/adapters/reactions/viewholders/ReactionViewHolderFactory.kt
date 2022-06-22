package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytItemAddReactionBinding
import com.sceyt.chat.ui.databinding.SceytItemReactionBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class ReactionViewHolderFactory(context: Context,
                                private val messageListeners: MessageClickListenersImpl?) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ReactionViewType.Reaction.ordinal -> {
                ReactionViewHolder(SceytItemReactionBinding.inflate(layoutInflater, parent, false),
                    messageListeners)
            }
            ReactionViewType.Add.ordinal -> {
                AddReactionViewHolder(SceytItemAddReactionBinding.inflate(layoutInflater, parent, false),
                    messageListeners)
            }
            else -> throw RuntimeException("Not supported view type")
        }
    }

    fun getItemViewType(position: Int, size: Int): Int {
        return when {
            position < size -> ReactionViewType.Reaction.ordinal
            else -> ReactionViewType.Add.ordinal
        }
    }

    enum class ReactionViewType {
        Add, Reaction
    }
}