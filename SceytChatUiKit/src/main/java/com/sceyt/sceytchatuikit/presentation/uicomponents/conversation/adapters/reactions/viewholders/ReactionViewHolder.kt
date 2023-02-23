package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytItemReactionBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle

class ReactionViewHolder(private val binding: SceytItemReactionBinding,
                         private val messageListeners: MessageClickListeners.ClickListeners?) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var reactionItem: ReactionItem.Reaction

    init {
        binding.root.setOnClickListener {
            messageListeners?.onReactionClick(it, reactionItem)
        }
    }

    fun bind(data: ReactionItem) {
        if (data !is ReactionItem.Reaction) return
        reactionItem = data

        with(binding.reactionView) {
            setCountAndSmile(data.reaction.score, data.reaction.key)
            if (data.reaction.containsSelf) {
                setReactionBgAndStrokeColor(getCompatColor(MessagesStyle.selfReactionBackgroundColor),
                    getCompatColor(MessagesStyle.selfReactionBorderColor))
            } else
                setReactionBgAndStrokeColor(0, getCompatColor(R.color.sceyt_color_divider))
        }
    }
}