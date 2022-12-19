package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.sceytchatuikit.databinding.ItemChannelFileBinding
import com.sceyt.sceytchatuikit.databinding.ItemChannelImageBinding
import com.sceyt.sceytchatuikit.databinding.ItemChannelVideoBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.FileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.ImageViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.VideoViewHolder

open class ChannelAttachmentViewHolderFactory(context: Context) {

    private val layoutInflater = LayoutInflater.from(context)
    private var clickListeners = AttachmentClickListenersImpl()

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
        return ImageViewHolder(
            ItemChannelImageBinding.inflate(layoutInflater, parent, false), clickListeners)
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseFileViewHolder {
        return VideoViewHolder(
            ItemChannelVideoBinding.inflate(layoutInflater, parent, false), clickListeners)
    }

    open fun createFileViewHolder(parent: ViewGroup): BaseFileViewHolder {
        return FileViewHolder(
            ItemChannelFileBinding.inflate(layoutInflater, parent, false), clickListeners)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseFileViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseFileViewHolder(binding.root) {
            override fun bind(item: FileListItem, payloads: MutableList<Any>?) {}
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

    fun setClickListener(listeners: AttachmentClickListeners) {
        clickListeners.setListener(listeners)
    }

    enum class ItemType {
        Image, Video, File, Loading
    }
}