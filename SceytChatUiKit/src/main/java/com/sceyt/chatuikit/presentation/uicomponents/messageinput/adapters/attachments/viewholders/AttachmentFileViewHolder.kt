package com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.viewholders

import android.content.res.ColorStateList
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemInputFileAttachmentBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

class AttachmentFileViewHolder(private val binding: SceytItemInputFileAttachmentBinding,
                               private val clickListeners: AttachmentClickListeners.ClickListeners) : BaseViewHolder<AttachmentItem>(binding.root) {

    init {
        binding.setupStyle()
    }

    override fun bind(item: AttachmentItem) {
        with(binding) {
            tvFileName.text = item.attachment.name
            tvFileSize.text = getFileSize(item.attachment.filePath).toPrettySize()
        }

        itemView.setOnClickListener { clickListeners.onRemoveAttachmentClick(it, item) }
    }

    private fun SceytItemInputFileAttachmentBinding.setupStyle() {
        icFile.setImageDrawable(MessagesListViewStyle.currentStyle?.messageItemStyle?.fileAttachmentIcon
                ?: context.getCompatDrawable(R.drawable.sceyt_ic_file_filled))
        icFile.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }
}