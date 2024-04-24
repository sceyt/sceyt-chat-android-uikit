package com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemInputVideoAttachmentBinding
import com.sceyt.chatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.shared.utils.FileResizeUtil

class AttachmentVideoViewHolder(private val binding: SceytItemInputVideoAttachmentBinding,
                                private val clickListeners: AttachmentClickListeners.ClickListeners) : BaseViewHolder<AttachmentItem>(binding.root) {

    init {
        binding.setStyle()
    }

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

    private fun SceytItemInputVideoAttachmentBinding.setStyle() {
        btnRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.surface3Color)
        layoutRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.backgroundColor)
    }
}