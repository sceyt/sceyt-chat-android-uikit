package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemInfoAllReactionsHeaderBinding
import com.sceyt.chatuikit.databinding.SceytItemInfoReactionHeaderBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.HeaderViewHolderFactory.OnItemClickListener
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.holders.header.AllReactionsViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.holders.header.ReactionsHeaderViewHolder
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.reactions_info.ReactionsInfoHeaderItemStyle

open class HeaderViewHolderFactory(
        private val style: ReactionsInfoHeaderItemStyle,
) {
    private var clickListener: OnItemClickListener = OnItemClickListener { _, _ -> }

    open fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReactionHeaderItem> {
        return when (viewType) {
            ViewType.Reaction.ordinal -> createReactionsHeaderViewHolder(parent)
            ViewType.All.ordinal -> createAllHeaderViewHolder(parent)
            else -> throw Exception("Unsupported view type")
        }
    }

    open fun getItemViewType(item: ReactionHeaderItem): Int {
        return when (item) {
            is ReactionHeaderItem.Reaction -> ViewType.Reaction.ordinal
            is ReactionHeaderItem.All -> ViewType.All.ordinal
        }
    }

    private fun createReactionsHeaderViewHolder(parent: ViewGroup): BaseViewHolder<ReactionHeaderItem> {
        val binding = SceytItemInfoReactionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReactionsHeaderViewHolder(binding, style, clickListener)
    }

    private fun createAllHeaderViewHolder(parent: ViewGroup): BaseViewHolder<ReactionHeaderItem> {
        val binding = SceytItemInfoAllReactionsHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllReactionsViewHolder(binding, style, clickListener)
    }

    enum class ViewType {
        Reaction,
        All
    }

    fun setClickListener(listener: OnItemClickListener) {
        clickListener = listener
    }

    fun interface OnItemClickListener {
        fun onItemClick(item: ReactionHeaderItem, position: Int)
    }
}