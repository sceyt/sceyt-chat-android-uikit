package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.viewholders

import android.content.res.ColorStateList
import com.sceyt.sceytchatuikit.databinding.SceytItemInputFileAttachmentBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle

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
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
        icFile.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}