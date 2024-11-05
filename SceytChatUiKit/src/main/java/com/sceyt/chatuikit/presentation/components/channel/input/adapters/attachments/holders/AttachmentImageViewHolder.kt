package com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.holders

import com.bumptech.glide.Glide
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemInputImageAttachmentBinding
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.AttachmentClickListeners
import com.sceyt.chatuikit.styles.input.InputSelectedMediaStyle

class AttachmentImageViewHolder(
        private val binding: SceytItemInputImageAttachmentBinding,
        private val clickListeners: AttachmentClickListeners.ClickListeners,
        private val style: InputSelectedMediaStyle
) : BaseViewHolder<AttachmentItem>(binding.root) {

    init {
        binding.setStyle()
    }

    override fun bind(item: AttachmentItem) {
        Glide.with(context)
            .load(item.attachment.filePath)
            .override(itemView.width)
            .into(binding.fileImage)

        itemView.setOnClickListener { clickListeners.onRemoveAttachmentClick(it, item) }
    }

    private fun SceytItemInputImageAttachmentBinding.setStyle() {
        layoutRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.backgroundColor)
        btnRemove.setImageDrawable(style.removeAttachmentIcon)
    }
}