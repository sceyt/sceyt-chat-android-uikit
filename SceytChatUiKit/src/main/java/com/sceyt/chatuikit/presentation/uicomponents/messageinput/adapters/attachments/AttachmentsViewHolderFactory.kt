package com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.databinding.SceytItemInputFileAttachmentBinding
import com.sceyt.chatuikit.databinding.SceytItemInputImageAttachmentBinding
import com.sceyt.chatuikit.databinding.SceytItemInputVideoAttachmentBinding
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.viewholders.AttachmentFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.viewholders.AttachmentImageViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.viewholders.AttachmentVideoViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig.userNameBuilder

open class AttachmentsViewHolderFactory(context: Context) {
    private val layoutInflater = LayoutInflater.from(context)
    private val clickListeners = AttachmentClickListenersImpl()

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AttachmentItem> {
        return when (viewType) {
            ItemType.Image.ordinal -> createImageViewHolder(parent)
            ItemType.Video.ordinal -> createVideoViewHolder(parent)
            ItemType.File.ordinal -> createFileViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createImageViewHolder(parent: ViewGroup): BaseViewHolder<AttachmentItem> {
        val binding = SceytItemInputImageAttachmentBinding.inflate(layoutInflater, parent, false)
        return AttachmentImageViewHolder(binding, clickListeners)
    }

    open fun createVideoViewHolder(parent: ViewGroup): BaseViewHolder<AttachmentItem> {
        val binding = SceytItemInputVideoAttachmentBinding.inflate(layoutInflater, parent, false)
        return AttachmentVideoViewHolder(binding, clickListeners)
    }

    open fun createFileViewHolder(parent: ViewGroup): BaseViewHolder<AttachmentItem> {
        val binding = SceytItemInputFileAttachmentBinding.inflate(layoutInflater, parent, false)
        return AttachmentFileViewHolder(binding, clickListeners)
    }

    open fun getItemViewType(item: AttachmentItem): Int {
        return when (item.attachment.type) {
            AttachmentTypeEnum.Image.value() -> ItemType.Image.ordinal
            AttachmentTypeEnum.Video.value() -> ItemType.Video.ordinal
            else -> ItemType.File.ordinal
        }
    }

    fun setClickListener(listeners: AttachmentClickListeners) {
        clickListeners.setListener(listeners)
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    enum class ItemType {
        Image, Video, File
    }
}