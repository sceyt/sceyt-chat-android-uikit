package com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.holders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemInputVideoAttachmentBinding
import com.sceyt.chatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import com.sceyt.chatuikit.styles.input.InputSelectedMediaStyle
import kotlin.time.Duration.Companion.milliseconds

class AttachmentVideoViewHolder(
        private val binding: SceytItemInputVideoAttachmentBinding,
        private val clickListeners: AttachmentClickListeners.ClickListeners,
        private val style: InputSelectedMediaStyle
) : BaseViewHolder<AttachmentItem>(binding.root) {

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
                        text = style.mediaDurationFormatter.format(context, durationMillis.milliseconds.inWholeSeconds)
                    isVisible = durationMillis > 0
                }
            }
        }

        itemView.setOnClickListener { clickListeners.onRemoveAttachmentClick(it, item) }
    }

    private fun SceytItemInputVideoAttachmentBinding.setStyle() {
        btnRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.iconSecondaryColor)
        layoutRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.backgroundColor)
        btnRemove.setImageDrawable(style.removeAttachmentIcon)
    }
}