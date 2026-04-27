package com.sceyt.chatuikit.presentation.components.global_search.links.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.persistence.differs.AttachmentDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.SearchLoadingMoreViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.links.adapter.holders.LinksSearchItemViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.holders.MediaSearchSectionViewHolder
import com.sceyt.chatuikit.styles.search.LinksSearchPageStyle

open class LinksSearchViewHolderFactory(
    context: Context,
    protected val style: LinksSearchPageStyle,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onAttachmentClick: (GlobalSearchListItem.AttachmentItem) -> Unit,
) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    open fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemType.Section.ordinal -> createSectionViewHolder(parent)
            ItemType.Link.ordinal -> createLinkViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingViewHolder(parent)
            else -> throw RuntimeException("Not supported view type: $viewType")
        }
    }

    open fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: GlobalSearchListItem,
    ) {
        when (holder) {
            is MediaSearchSectionViewHolder -> holder.bind(item as GlobalSearchListItem.DateSeparator)
            is LinksSearchItemViewHolder -> holder.bind(item as GlobalSearchListItem.AttachmentItem)
        }
    }

    open fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: GlobalSearchListItem,
        diff: AttachmentDiff,
    ) {
        when (holder) {
            is MediaSearchSectionViewHolder -> holder.bind(item as GlobalSearchListItem.DateSeparator)
            is LinksSearchItemViewHolder -> holder.bind(item as GlobalSearchListItem.AttachmentItem)
        }
    }

    open fun createSectionViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return MediaSearchSectionViewHolder(
            style = style.dateSeparatorStyle,
            binding = SceytItemGlobalSearchSectionBinding.inflate(layoutInflater, parent, false),
        )
    }

    open fun createLinkViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return LinksSearchItemViewHolder(
            style = style.linkItemStyle,
            binding = SceytItemChannelLinkBinding.inflate(layoutInflater, parent, false),
            needMediaDataCallback = needMediaDataCallback,
            onAttachmentClickListener = onAttachmentClick,
        )
    }

    open fun createLoadingViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return SearchLoadingMoreViewHolder(parent)
    }

    open fun getItemViewType(item: GlobalSearchListItem, position: Int): Int {
        return when (item) {
            is GlobalSearchListItem.DateSeparator -> ItemType.Section.ordinal
            is GlobalSearchListItem.AttachmentItem -> ItemType.Link.ordinal
            is GlobalSearchListItem.Loading -> ItemType.Loading.ordinal
            else -> throw RuntimeException("Not supported item type: $item")
        }
    }

    enum class ItemType {
        Section, Link, Loading
    }
}
