package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.databinding.SceytItemReactionBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners

class ReactionViewHolderFactory(context: Context,
                                private val messageListeners: MessageClickListeners.ClickListeners?) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return ReactionViewHolder(SceytItemReactionBinding.inflate(layoutInflater, parent, false),
            messageListeners)
    }
}