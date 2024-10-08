package com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemInputFileAttachmentBinding
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.toSceytAttachment
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.input.InputSelectedMediaStyle

class AttachmentFileViewHolder(
        private val binding: SceytItemInputFileAttachmentBinding,
        private val clickListeners: AttachmentClickListeners.ClickListeners,
        private val style: InputSelectedMediaStyle
) : BaseViewHolder<AttachmentItem>(binding.root) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: AttachmentItem) {
        with(binding) {
            tvFileName.text = item.attachment.name
            val sceytAttachment = item.attachment.toSceytAttachment(
                messageTid = 0L,
                transferState = TransferState.PendingUpload)

            tvFileSize.text = style.fileAttachmentSizeFormatter.format(context, sceytAttachment)

            style.fileAttachmentIconProvider.provide(context, sceytAttachment)?.let {
                icFile.setImageDrawable(it)
            }
        }

        itemView.setOnClickListener { clickListeners.onRemoveAttachmentClick(it, item) }
    }

    private fun SceytItemInputFileAttachmentBinding.applyStyle() {
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
        layoutRemove.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.backgroundColor)
        imageCont.setCardBackgroundColor(style.fileAttachmentBackgroundColor)
        btnRemove.setImageDrawable(style.removeAttachmentIcon)
        style.fileAttachmentNameTextStyle.apply(tvFileName)
        style.fileAttachmentSizeTextStyle.apply(tvFileSize)
    }
}