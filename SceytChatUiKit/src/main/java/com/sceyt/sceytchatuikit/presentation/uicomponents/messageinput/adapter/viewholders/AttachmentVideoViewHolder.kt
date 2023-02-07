package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.databinding.SceytItemInputVideoAttachmentBinding
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.AttachmentItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil

class AttachmentVideoViewHolder(private val binding: SceytItemInputVideoAttachmentBinding,
                                private val clickListeners: AttachmentClickListeners.ClickListeners) : BaseViewHolder<AttachmentItem>(binding.root) {

    override fun bind(item: AttachmentItem) {
        with(binding) {
            if (item.attachment.type.isEqualsVideoOrImage()) {
                Glide.with(context)
                    .load(item.attachment.filePath)
                    .override(itemView.width)
                    .into(fileImage)

                val durationMillis = FileResizeUtil.getVideoDuration(context, item.attachment.filePath)
                        ?: 0

                tvDuration.apply {
                    if (durationMillis > 0)
                        text = DateTimeUtil.secondsToTime(durationMillis / 1000)
                    isVisible = durationMillis > 0
                }
            }
        }

        itemView.setOnClickListener { clickListeners.onRemoveAttachmentClick(it, item) }
    }
}