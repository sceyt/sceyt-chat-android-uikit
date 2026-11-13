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
import com.sceyt.chatuikit.styles.preview.MediaPreviewStyle

open class MediaFilesViewHolderFactory(
    context: Context,
    private val style: MediaPreviewStyle
) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var clickListeners: (MediaItem) -> Unit = {}
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}
    private var loaderClickListener: (MediaItem) -> Unit = {}

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<MediaItem> {
        return when (viewType) {
            ItemType.Image.ordinal -> createImageViewHolder(parent)
            ItemType.Video.ordinal -> createVideoViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createImageViewHolder(parent: ViewGroup): BaseFileViewHolder<MediaItem> {
        return MediaImageViewHolder(
            binding = SceytMediaItemImageBinding.inflate(layoutInflater, parent, false),
            style = style,
            clickListeners = clickListeners,
            needMediaDataCallback = needMediaDataCallback,
            loaderClickListener = loaderClickListener
        )
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseFileViewHolder<MediaItem> {
        return MediaVideoViewHolder(
            binding = SceytMediaItemVideoBinding.inflate(layoutInflater, parent, false),
            style = style,
            clickListeners = clickListeners,
            needMediaDataCallback = needMediaDataCallback,
            loaderClickListener = loaderClickListener
        )
    }

    open fun getItemViewType(item: MediaItem): Int {
        return when (item.type) {
            MediaItemType.Image -> ItemType.Image.ordinal
            MediaItemType.Video -> ItemType.Video.ordinal
        }
    }

    fun setClickListener(listeners: (MediaItem) -> Unit) {
        clickListeners = listeners
    }

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    fun setLoaderClickListener(listener: (MediaItem) -> Unit) {
        loaderClickListener = listener
    }

    @Suppress("unused")
    protected fun getNeedMediaDataCallback() = needMediaDataCallback

    enum class ItemType {
        Image, Video, Loading
    }
}