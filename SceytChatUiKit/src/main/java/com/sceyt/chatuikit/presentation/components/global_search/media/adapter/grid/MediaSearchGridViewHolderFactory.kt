package com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid

import android.content.Context
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.databinding.SceytItemChannelImageBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelVideoBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.SearchLoadingMoreViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid.holders.MediaSearchGridImageViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid.holders.MediaSearchGridVideoViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.holders.MediaSearchSectionViewHolder
import com.sceyt.chatuikit.styles.search.MediaSearchPageStyle
import java.util.Date

open class MediaSearchGridViewHolderFactory(
    private val context: Context,
    private val style: MediaSearchPageStyle,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onItemClick: (View, GlobalSearchAttachmentResult) -> Unit,
) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    open fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemType.Image.ordinal -> createImageViewHolder(parent)
            ItemType.Video.ordinal -> createVideoViewHolder(parent)
            ItemType.DateSeparator.ordinal -> createDateSeparatorViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingViewHolder(parent)
            else -> throw RuntimeException("Not supported view type: $viewType")
        }
    }

    open fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: GlobalSearchListItem) {
        when (holder) {
            is MediaSearchGridImageViewHolder -> holder.bind(item as GlobalSearchListItem.AttachmentItem)
            is MediaSearchGridVideoViewHolder -> holder.bind(item as GlobalSearchListItem.AttachmentItem)
            is MediaSearchSectionViewHolder -> holder.bind(item as GlobalSearchListItem.DateSeparator)
        }
    }

    open fun createLoadingViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return SearchLoadingMoreViewHolder(parent)
    }

    open fun getItemViewType(item: GlobalSearchListItem): Int {
        return when (item) {
            is GlobalSearchListItem.AttachmentItem -> {
                when (item.result.attachment.type) {
                    AttachmentTypeEnum.Video.value -> ItemType.Video.ordinal
                    else -> ItemType.Image.ordinal
                }
            }

            is GlobalSearchListItem.DateSeparator -> ItemType.DateSeparator.ordinal
            is GlobalSearchListItem.Loading -> ItemType.Loading.ordinal
            else -> throw RuntimeException("Not supported item type: $item")
        }
    }

    open fun createImageViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return MediaSearchGridImageViewHolder(
            binding = SceytItemChannelImageBinding.inflate(layoutInflater, parent, false),
            style = style.itemStyle,
            mediaDataCallback = needMediaDataCallback,
            onItemClick = onItemClick,
        )
    }

    open fun createVideoViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return MediaSearchGridVideoViewHolder(
            binding = SceytItemChannelVideoBinding.inflate(layoutInflater, parent, false),
            style = style.itemStyle,
            mediaDataCallback = needMediaDataCallback,
            onItemClick = onItemClick,
        )
    }

    open fun createDateSeparatorViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return MediaSearchSectionViewHolder(
            binding = SceytItemGlobalSearchSectionBinding.inflate(layoutInflater, parent, false),
            style = style.dateSeparatorStyle,
        )
    }

    fun bindDateSeparatorHeader(
        binding: SceytItemGlobalSearchSectionBinding,
        item: GlobalSearchListItem,
    ) {
        style.dateSeparatorStyle.apply {
            textStyle.apply(binding.root)
            backgroundStyle.apply(binding.root)
            binding.root.text = dateFormatter.format(context, Date(item.getCreatedAt()))
        }
    }

    enum class ItemType {
        Image, Video, DateSeparator, Loading
    }
}
