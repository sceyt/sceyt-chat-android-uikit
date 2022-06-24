package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.ui.databinding.ItemChannelFileBinding
import com.sceyt.chat.ui.databinding.ItemChannelImageBinding
import com.sceyt.chat.ui.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.adapter.FileViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.viewholder.ImageViewHolder

open class ChannelAttachmentViewHolderFactory(context: Context) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder {
        return when (viewType) {
            ItemType.Image.ordinal -> createImageViewHolder(parent)
            ItemType.Video.ordinal -> createVideoViewHolder(parent)
            ItemType.File.ordinal -> createFileViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createImageViewHolder(parent: ViewGroup): BaseFileViewHolder {
        return ImageViewHolder(ItemChannelImageBinding.inflate(layoutInflater, parent, false))
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseFileViewHolder {
        return ImageViewHolder(ItemChannelImageBinding.inflate(layoutInflater, parent, false))
    }

    open fun createFileViewHolder(parent: ViewGroup): BaseFileViewHolder {
        return FileViewHolder(ItemChannelFileBinding.inflate(layoutInflater, parent, false))
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseFileViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseFileViewHolder(binding.root) {
            override fun bind(item: FileListItem) {

            }
        }
    }

    open fun getItemViewType(item: FileListItem): Int {
        return when (item) {
            is FileListItem.Image -> ItemType.Image.ordinal
            is FileListItem.Video -> ItemType.Video.ordinal
            is FileListItem.File -> ItemType.File.ordinal
            is FileListItem.LoadingMoreItem -> ItemType.Loading.ordinal
        }
    }

    enum class ItemType {
        Image, Video, File, Loading
    }
}