package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelImageBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelMediaDateBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelVideoBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.ChannelMediaDateViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.FileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.ImageViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.LinkViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.VideoViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.VoiceViewHolder
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.ConversationInfoMediaStyle
import com.sceyt.chatuikit.shared.helpers.LinkPreviewHelper

open class ChannelAttachmentViewHolderFactory(context: Context,
                                              protected val style: ConversationInfoMediaStyle) {

    protected val layoutInflater = LayoutInflater.from(context)
    protected var clickListeners = AttachmentClickListenersImpl()
        private set
    protected var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
        private set
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}
    protected var linkPreviewHelper: LinkPreviewHelper? = null
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
            SceytItemChannelImageBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback)
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return VideoViewHolder(
            SceytItemChannelVideoBinding.inflate(layoutInflater, parent, false),
            style, clickListeners, needMediaDataCallback = needMediaDataCallback)
    }

    open fun createFileViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return FileViewHolder(
            SceytItemChannelFileBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback)
    }

    open fun createVoiceViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return VoiceViewHolder(
            SceytItemChannelVoiceBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback, userNameBuilder = userNameBuilder)
    }

    open fun createLinkViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        return LinkViewHolder(
            SceytItemChannelLinkBinding.inflate(layoutInflater, parent, false), linkPreviewHelper,
            clickListeners)
    }

    open fun createMediaDateViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        val binding = SceytItemChannelMediaDateBinding.inflate(layoutInflater, parent, false)
        return ChannelMediaDateViewHolder(binding, style)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseFileViewHolder<ChannelFileItem> {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {
            override fun bind(item: ChannelFileItem) {
            }
        }
    }

    open fun getItemViewType(item: ChannelFileItem): Int {
        return when (item) {
            is ChannelFileItem.Image -> ItemType.Image.ordinal
            is ChannelFileItem.Video -> ItemType.Video.ordinal
            is ChannelFileItem.File -> ItemType.File.ordinal
            is ChannelFileItem.Voice -> ItemType.Voice.ordinal
            is ChannelFileItem.Link -> ItemType.Link.ordinal
            is ChannelFileItem.MediaDate -> ItemType.MediaDate.ordinal
            is ChannelFileItem.LoadingMoreItem -> ItemType.Loading.ordinal
        }
    }

    fun setClickListener(listeners: AttachmentClickListeners) {
        clickListeners.setListener(listeners)
    }

    fun getClickListeners() = clickListeners as AttachmentClickListeners.ClickListeners

    fun getMediaStyle() = style

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    fun setLinkPreviewHelper(helper: LinkPreviewHelper) {
        linkPreviewHelper = helper
    }

    protected fun getNeedMediaDataCallback() = needMediaDataCallback

    enum class ItemType {
        Image, Video, File, Voice, Link, MediaDate, Loading
    }
}