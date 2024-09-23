package com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemInputFileAttachmentBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.toSceytAttachment
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.MessageInputStyle

class AttachmentFileViewHolder(
        private val binding: SceytItemInputFileAttachmentBinding,
        private val inputStyle: MessageInputStyle,
        private val clickListeners: AttachmentClickListeners.ClickListeners
) : BaseViewHolder<AttachmentItem>(binding.root) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: AttachmentItem) {
        with(binding) {
            tvFileName.text = item.attachment.name
            tvFileSize.text = getFileSize(item.attachment.filePath.toString()).toPrettySize()

            inputStyle.selectedAttachmentIconProvider.provide(item.attachment.toSceytAttachment(
                messageTid = 0L,
                transferState = TransferState.PendingUpload))?.let {
                icFile.setImageDrawable(it)
            }
        }

        itemView.setOnClickListener { clickListeners.onRemoveAttachmentClick(it, item) }
    }

    private fun SceytItemInputFileAttachmentBinding.applyStyle() {
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.accentColor)
        imageCont.setCardBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.surface1Color))
        btnRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
        layoutRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.backgroundColor)
    }
}