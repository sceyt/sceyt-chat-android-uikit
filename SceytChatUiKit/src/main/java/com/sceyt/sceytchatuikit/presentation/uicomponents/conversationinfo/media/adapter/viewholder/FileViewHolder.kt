package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.sceyt.sceytchatuikit.databinding.ItemChannelFileBinding
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.*

class FileViewHolder(private val binding: ItemChannelFileBinding,
                     private val clickListeners: AttachmentClickListenersImpl) : BaseFileViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        val file = (item as? FileListItem.File)?.file ?: return

        with(binding) {
            tvFileName.text = file.name

            val sizeText = if (item.message.incoming) {
                file.fileSize.toPrettySize()
            } else {
                val size = if (file.fileSize == 0L) {
                    file.url?.let { getFileSize(it) } ?: 0L
                } else file.fileSize

                size.toPrettySize()
            }

            val date = DateTimeUtil.convertDateToString(Date(item.message.createdAt), "dd/MM/yyyy")
            val sizeAndDate = "$sizeText, $date"
            tvFileSizeAndDate.text = sizeAndDate
        }
    }
}