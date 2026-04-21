package com.sceyt.chatuikit.presentation.components.global_search.voice.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.persistence.differs.AttachmentDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.media.adapter.holders.MediaSearchSectionViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.voice.adapter.holders.VoiceSearchItemViewHolder
import com.sceyt.chatuikit.styles.search.VoiceSearchPageStyle

open class VoiceSearchViewHolderFactory(
    context: Context,
    protected val style: VoiceSearchPageStyle,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onAttachmentClick: (GlobalSearchListItem.AttachmentItem) -> Unit,
    private val onAttachmentLoaderClick: ((GlobalSearchListItem.AttachmentItem) -> Unit)? = null,
) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    open fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemType.Section.ordinal -> createSectionViewHolder(parent)
            ItemType.Voice.ordinal -> createVoiceViewHolder(parent)
            else -> throw RuntimeException("Not supported view type: $viewType")
        }
    }

    open fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: GlobalSearchListItem,
    ) {
        when (holder) {
            is MediaSearchSectionViewHolder -> holder.bind(item as GlobalSearchListItem.DateSeparator)
            is VoiceSearchItemViewHolder -> holder.bind(item as GlobalSearchListItem.AttachmentItem)
        }
    }

    open fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: GlobalSearchListItem,
        diff: AttachmentDiff,
    ) {
        when (holder) {
            is MediaSearchSectionViewHolder -> holder.bind(item as GlobalSearchListItem.DateSeparator)
            is VoiceSearchItemViewHolder -> holder.bind(item as GlobalSearchListItem.AttachmentItem)
        }
    }

    open fun createSectionViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return MediaSearchSectionViewHolder(
            style = style.dateSeparatorStyle,
            binding = SceytItemGlobalSearchSectionBinding.inflate(layoutInflater, parent, false),
        )
    }

    open fun createVoiceViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return VoiceSearchItemViewHolder(
            style = style.voiceItemStyle,
            binding = SceytItemChannelVoiceBinding.inflate(layoutInflater, parent, false),
            needMediaDataCallback = needMediaDataCallback,
            onAttachmentClickListener = onAttachmentClick,
            onAttachmentLoaderClickListener = onAttachmentLoaderClick,
        )
    }

    open fun getItemViewType(item: GlobalSearchListItem, position: Int): Int {
        return when (item) {
            is GlobalSearchListItem.DateSeparator -> ItemType.Section.ordinal
            is GlobalSearchListItem.AttachmentItem -> ItemType.Voice.ordinal
            else -> throw RuntimeException("Not supported item type: $item")
        }
    }

    enum class ItemType {
        Section, Voice
    }
}
