package com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.viewholders

import com.bumptech.glide.Glide
import com.sceyt.chatuikit.databinding.SceytItemInputImageAttachmentBinding
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners

class AttachmentImageViewHolder(private val binding: SceytItemInputImageAttachmentBinding,
                                private val clickListeners: AttachmentClickListeners.ClickListeners) : BaseViewHolder<AttachmentItem>(binding.root) {

    override fun bind(item: AttachmentItem) {
        Glide.with(context)
            .load(item.attachment.filePath)
            .override(itemView.width)
            .into(binding.fileImage)

        itemView.setOnClickListener { clickListeners.onRemoveAttachmentClick(it, item) }
    }
}