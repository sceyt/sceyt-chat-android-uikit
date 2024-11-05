package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.holders.header

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytItemInfoAllReactionsHeaderBinding
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.HeaderViewHolderFactory.OnItemClickListener
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ReactionHeaderItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.reactions_info.ReactionsInfoHeaderItemStyle

class AllReactionsViewHolder(
        val binding: SceytItemInfoAllReactionsHeaderBinding,
        private val style: ReactionsInfoHeaderItemStyle,
        private val clickListener: OnItemClickListener
) : BaseViewHolder<ReactionHeaderItem>(binding.root) {

    @SuppressLint("SetTextI18n")
    override fun bind(item: ReactionHeaderItem) {
        with(binding.tvAll) {
            text = "${itemView.getString(R.string.sceyt_all)} ${(item as ReactionHeaderItem.All).count}"

            if (item.selected) {
                background = GradientDrawable().apply {
                    color = ColorStateList.valueOf(style.selectedBackgroundColor)
                    cornerRadius = style.cornerRadius.toFloat()
                    setStroke(style.borderWidth, style.borderColor)
                }
                style.selectedTextStyle.apply(this)
            } else {
                background = GradientDrawable().apply {
                    color = ColorStateList.valueOf(style.backgroundColor)
                    cornerRadius = style.cornerRadius.toFloat()
                    setStroke(style.borderWidth, style.borderColor)
                }
                style.textStyle.apply(this)
            }

            binding.root.setOnClickListener {
                clickListener.onItemClick(item, bindingAdapterPosition)
            }
        }
    }
}