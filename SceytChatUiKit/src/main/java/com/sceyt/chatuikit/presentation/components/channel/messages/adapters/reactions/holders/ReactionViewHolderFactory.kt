package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemReactionBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners

class ReactionViewHolderFactory(
        context: Context,
        private val onReactionClickListener: MessageClickListeners.ReactionClickListener?,
) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return ReactionViewHolder(SceytItemReactionBinding.inflate(layoutInflater, parent, false),
            onReactionClickListener)
    }
}