package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelImageBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelMediaDateSeparatorBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelVideoBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItemType
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.ChannelMediaDateSeparatorViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.FileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.ImageViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.LinkViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.VideoViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders.VoiceViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle

open class ChannelAttachmentViewHolderFactory(
        context: Context,
        val style: ChannelInfoStyle,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
) {
    protected val layoutInflater = LayoutInflater.from(context)
    protected var clickListeners = AttachmentClickListenersImpl()
        private set
    protected var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}
        private set

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<ChannelFileItem> {
        return when (viewType) {
            ItemType.Image.ordinal -> createImageViewHolder(parent)
            ItemType.Video.ordinal -> createVideoViewHolder(parent)
            ItemType.File.ordinal -> createFileViewHolder(parent)
            ItemType.Voice.ordinal -> createVoiceViewHolder(parent)
            ItemType.Link.ordinal -> createLinkViewHolder(parent)
            ItemType.MediaDate.ordinal -> createMediaDateViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createImageViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return ImageViewHolder(
            SceytItemChannelImageBinding.inflate(layoutInflater, parent, false),
            style.mediaStyle.itemStyle, clickListeners, needMediaDataCallback = needMediaDataCallback)
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return VideoViewHolder(
            SceytItemChannelVideoBinding.inflate(layoutInflater, parent, false),
            style.mediaStyle.itemStyle, clickListeners, needMediaDataCallback = needMediaDataCallback)
    }

    open fun createFileViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return FileViewHolder(
            SceytItemChannelFileBinding.inflate(layoutInflater, parent, false),
            style.filesStyle.itemStyle, clickListeners, needMediaDataCallback = needMediaDataCallback)
    }

    open fun createVoiceViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return VoiceViewHolder(
            SceytItemChannelVoiceBinding.inflate(layoutInflater, parent, false),
            style.voiceStyle.itemStyle, clickListeners, needMediaDataCallback = needMediaDataCallback)
    }

    open fun createLinkViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return LinkViewHolder(
            SceytItemChannelLinkBinding.inflate(layoutInflater, parent, false),
            style.linkStyle.itemStyle, clickListeners, needMediaDataCallback)
    }

    open fun createMediaDateViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        val binding = SceytItemChannelMediaDateSeparatorBinding.inflate(layoutInflater, parent, false)
        return ChannelMediaDateSeparatorViewHolder(binding, dateSeparatorStyle)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {
            override fun bind(item: ChannelFileItem) {
                binding.adapterListLoadingProgressBar.setProgressColor(SceytChatUIKit.theme.colors.accentColor)
            }
        }
    }

    open fun getItemViewType(item: ChannelFileItem): Int {
        return when (item) {
            is ChannelFileItem.LoadingMoreItem -> ItemType.Loading.ordinal
            is ChannelFileItem.Item -> {
                when (item.type) {
                    ChannelFileItemType.Image -> ItemType.Image.ordinal
                    ChannelFileItemType.Video -> ItemType.Video.ordinal
                    ChannelFileItemType.File -> ItemType.File.ordinal
                    ChannelFileItemType.Voice -> ItemType.Voice.ordinal
                    ChannelFileItemType.Link -> ItemType.Link.ordinal
                    ChannelFileItemType.MediaDate -> ItemType.MediaDate.ordinal
                }
            }
        }
    }

    fun setClickListener(listeners: AttachmentClickListeners) {
        clickListeners.setListener(listeners)
    }

    fun getClickListeners() = clickListeners as AttachmentClickListeners.ClickListeners

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    enum class ItemType {
        Image, Video, File, Voice, Link, MediaDate, Loading
    }
}