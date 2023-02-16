package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytMediaItemImageBinding
import com.sceyt.sceytchatuikit.databinding.SceytMediaItemVideoBinding
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders.MediaImageViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders.MediaVideoViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.userNameBuilder

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

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    protected fun getNeedMediaDataCallback() = needMediaDataCallback

    enum class ItemType {
        Image, Video, Loading
    }
}