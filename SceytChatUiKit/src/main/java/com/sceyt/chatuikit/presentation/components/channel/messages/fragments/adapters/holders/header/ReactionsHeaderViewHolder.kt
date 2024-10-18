package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.holders.header

import com.sceyt.chatuikit.databinding.SceytItemInfoReactionHeaderBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.HeaderViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ReactionHeaderItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.reactions_info.ReactionsInfoHeaderItemStyle

class ReactionsHeaderViewHolder(
        private val binding: SceytItemInfoReactionHeaderBinding,
        private val style: ReactionsInfoHeaderItemStyle,
        private val clickListener: HeaderViewHolderFactory.OnItemClickListener
) : BaseViewHolder<ReactionHeaderItem>(binding.root) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: ReactionHeaderItem) {
        val score = (item as ReactionHeaderItem.Reaction).reactionTotal

        with(binding.reaction) {
            setCountAndSmile(score.score, score.key)

            if (item.selected) {
                setReactionBackgroundColor(style.selectedBackgroundColor)
                setCountTextStyle(style.selectedTextStyle)
            } else {
                setReactionBackgroundColor(style.backgroundColor)
                setCountTextStyle(style.textStyle)
            }
        }

        binding.root.setOnClickListener {
            clickListener.onItemClick(item, bindingAdapterPosition)
        }
    }

    private fun SceytItemInfoReactionHeaderBinding.applyStyle() {
        reaction.setStroke(style.borderColor, style.borderWidth, style.cornerRadius)
    }
}