package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseChannelFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

open class ChannelAttachmentViewHolderFactory(context: Context,
                                              private val linkPreviewHelper: LinkPreviewHelper? = null) {

    protected val layoutInflater = LayoutInflater.from(context)
    private var clickListeners = AttachmentClickListenersImpl()
    private var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
    private var needMediaDataCallback: (NeedMediaInfoData) -> Unit = {}

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseChannelFileViewHolder {
        return when (viewType) {
            ItemType.Image.ordinal -> createImageViewHolder(parent)
            ItemType.Video.ordinal -> createVideoViewHolder(parent)
            ItemType.File.ordinal -> createFileViewHolder(parent)
            ItemType.Voice.ordinal -> createVoiceViewHolder(parent)
            ItemType.Link.ordinal -> createLinkViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createImageViewHolder(parent: ViewGroup): BaseChannelFileViewHolder {
        return ImageViewHolder(
            SceytItemChannelImageBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback)
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseChannelFileViewHolder {
        return VideoViewHolder(
            SceytItemChannelVideoBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback)
    }

    open fun createFileViewHolder(parent: ViewGroup): BaseChannelFileViewHolder {
        return FileViewHolder(
            SceytItemChannelFileBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback)
    }

    open fun createVoiceViewHolder(parent: ViewGroup): BaseChannelFileViewHolder {
        return VoiceViewHolder(
            SceytItemChannelVoiceBinding.inflate(layoutInflater, parent, false), clickListeners,
            needMediaDataCallback = needMediaDataCallback, userNameBuilder = userNameBuilder)
    }

    open fun createLinkViewHolder(parent: ViewGroup): BaseChannelFileViewHolder {
        return LinkViewHolder(
            SceytItemChannelLinkBinding.inflate(layoutInflater, parent, false), linkPreviewHelper,
            clickListeners)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseChannelFileViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseChannelFileViewHolder(binding.root, {}) {
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
            is ChannelFileItem.LoadingMoreItem -> ItemType.Loading.ordinal
        }
    }

    fun setClickListener(listeners: AttachmentClickListeners) {
        clickListeners.setListener(listeners)
    }

    fun getClickListeners() = clickListeners as AttachmentClickListeners.ClickListeners

    fun setNeedMediaDataCallback(callback: (NeedMediaInfoData) -> Unit) {
        needMediaDataCallback = callback
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    protected fun getNeedMediaDataCallback() = needMediaDataCallback

    enum class ItemType {
        Image, Video, File, Voice, Link, Loading
    }
}