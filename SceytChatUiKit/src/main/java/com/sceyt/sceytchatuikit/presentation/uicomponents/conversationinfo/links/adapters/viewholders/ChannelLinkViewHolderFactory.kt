package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.sceytchatuikit.databinding.ItemChannelLinkBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

open class ChannelLinkViewHolderFactory(context: Context,
                                        private val linkPreview: LinkPreviewHelper,
                                        private val clickListener: LinkClickListener) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<LinkItem> {
        return when (viewType) {
            ItemType.Link.ordinal -> createLinkViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createLinkViewHolder(parent: ViewGroup): com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<LinkItem> {
        return LinkViewHolder(
            ItemChannelLinkBinding.inflate(layoutInflater, parent, false),
            linkPreview, clickListener)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<LinkItem> {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<LinkItem>(binding.root) {
            override fun bind(item: LinkItem) {

            }
        }
    }

    open fun getItemViewType(item: LinkItem): Int {
        return when (item) {
            is LinkItem.Link -> ItemType.Link.ordinal
            is LinkItem.LoadingMore -> ItemType.Loading.ordinal
        }
    }

    enum class ItemType {
        Link, Loading
    }

    fun interface LinkClickListener {
        fun onItemClick(listItem: LinkItem.Link)
    }
}