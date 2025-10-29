package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemVoterBinding
import com.sceyt.chatuikit.databinding.SceytItemVoterHeaderBinding
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListeners
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListenersImpl
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.poll_results.VoterItemStyle

open class VotersViewHolderFactory(
        private val context: Context,
        private val style: VoterItemStyle,
) {
    private val layoutInflater = LayoutInflater.from(context)
    private val clickListeners = VoterClickListenersImpl()

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VoterItem> {
        return when (viewType) {
            ItemType.Header.ordinal -> VoterHeaderViewHolder(
                binding = SceytItemVoterHeaderBinding.inflate(layoutInflater, parent, false),
                style = style
            )

            ItemType.Voter.ordinal -> VoterViewHolder(
                binding = SceytItemVoterBinding.inflate(layoutInflater, parent, false),
                style = style,
                clickListeners = clickListeners
            )

            ItemType.Loading.ordinal -> LoadingMoreViewHolder(
                SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false),
            )

            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    fun getItemViewType(item: VoterItem): Int {
        return when (item) {
            is VoterItem.HeaderItem -> ItemType.Header.ordinal
            is VoterItem.Voter -> ItemType.Voter.ordinal
            is VoterItem.LoadingMore -> ItemType.Loading.ordinal
        }
    }

    fun setOnClickListener(listener: VoterClickListeners) {
        clickListeners.setListener(listener)
    }

    private enum class ItemType {
        Header, Voter, Loading
    }

    private class LoadingMoreViewHolder(
            private val binding: SceytItemLoadingMoreBinding,
    ) : BaseViewHolder<VoterItem>(binding.root) {
        override fun bind(item: VoterItem) {}
    }
}