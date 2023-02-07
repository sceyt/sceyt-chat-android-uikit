package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.viewholders

import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.databinding.SceytItemInputImageAttachmentBinding
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.AttachmentItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners

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