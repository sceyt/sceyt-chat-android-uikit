package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.ui.databinding.ItemChannelLinkBinding
import com.sceyt.chat.ui.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chat.ui.presentation.common.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper

open class ChannelLinkViewHolderFactory(context: Context,
                                        private val linkPreview: LinkPreviewHelper,
                                        private val clickListener: LinkClickListener) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<LinkItem> {
        return when (viewType) {
            ItemType.Link.ordinal -> createLinkViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createLinkViewHolder(parent: ViewGroup): BaseViewHolder<LinkItem> {
        return LinkViewHolder(
            ItemChannelLinkBinding.inflate(layoutInflater, parent, false),
            linkPreview, clickListener)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseViewHolder<LinkItem> {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseViewHolder<LinkItem>(binding.root) {
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