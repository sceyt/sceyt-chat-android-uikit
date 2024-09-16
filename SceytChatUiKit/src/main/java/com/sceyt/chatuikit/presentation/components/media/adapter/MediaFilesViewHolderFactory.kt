package com.sceyt.chatuikit.presentation.components.media.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytMediaItemImageBinding
import com.sceyt.chatuikit.databinding.SceytMediaItemVideoBinding
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.media.adapter.holders.MediaImageViewHolder
import com.sceyt.chatuikit.presentation.components.media.adapter.holders.MediaVideoViewHolder

open class MediaFilesViewHolderFactory(context: Context) {
    protected val layoutInflater = LayoutInflater.from(context)
    private var clickListeners: (MediaItem) -> Unit = {}
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<MediaItem> {
        return when (viewType) {
            ItemType.Image.ordinal -> createImageViewHolder(parent)
            ItemType.Video.ordinal -> createVideoViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createImageViewHolder(parent: ViewGroup): BaseFileViewHolder<MediaItem> {
        return MediaImageViewHolder(
            SceytMediaItemImageBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback)
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseFileViewHolder<MediaItem> {
        return MediaVideoViewHolder(
            SceytMediaItemVideoBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback)
    }

    open fun getItemViewType(item: MediaItem): Int {
        return when (item) {
            is MediaItem.Image -> ItemType.Image.ordinal
            is MediaItem.Video -> ItemType.Video.ordinal
        }
    }

    fun setClickListener(listeners: (MediaItem) -> Unit) {
        clickListeners = listeners
    }

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    protected fun getNeedMediaDataCallback() = needMediaDataCallback

    enum class ItemType {
        Image, Video, Loading
    }
}