package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.adapter

import com.sceyt.chat.ui.databinding.ItemChannelFileBinding
import com.sceyt.chat.ui.extensions.getFileSize
import com.sceyt.chat.ui.extensions.toPrettySize
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chat.ui.utils.DateTimeUtil
import java.util.*

class FileViewHolder(private val binding: ItemChannelFileBinding) : BaseFileViewHolder(binding.root) {

    override fun bind(item: FileListItem) {
        super.bind(item)
        val file = (item as? FileListItem.File)?.file ?: return

        with(binding) {
            tvFileName.text = file.name

            if (item.message.incoming) {
                tvFileSize.text = file.uploadedFileSize.toPrettySize()
            } else {
                val size = if (file.uploadedFileSize == 0L) {
                    getFileSize(file.url)
                } else file.uploadedFileSize

                tvFileSize.text = size.toPrettySize()
                tvDate.text = DateTimeUtil.convertDateToString(Date(item.message.createdAt), "dd/mm/yyyy")
            }
        }
    }
}