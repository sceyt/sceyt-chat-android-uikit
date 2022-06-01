package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import com.sceyt.chat.ui.databinding.SceytMessageFileItemBinding
import com.sceyt.chat.ui.extensions.getFileSize
import com.sceyt.chat.ui.extensions.toPrettySize
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl

class MessageFileViewHolder(
        private val binding: SceytMessageFileItemBinding,
        private val messageListeners: MessageClickListenersImpl?
) : BaseFileViewHolder(binding.root) {

    override fun bindViews(item: FileListItem) {
        val fileItem = (item as? FileListItem.File)?.file ?: return

        with(binding) {
            loadData = item.fileLoadData
            tvFileName.text = fileItem.name

            if (item.message.incoming) {
                tvFileSize.text = fileItem.uploadedFileSize.toPrettySize()
            } else {
                val size = if (fileItem.uploadedFileSize == 0L) {
                    getFileSize(fileItem.url)
                } else fileItem.uploadedFileSize

                tvFileSize.text = size.toPrettySize()
            }

            setUploadListenerIfNeeded(item)
            downloadIfNeeded(item)

            root.setOnClickListener {
                messageListeners?.onAttachmentClick(it, item)
            }

            root.setOnLongClickListener {
                messageListeners?.onAttachmentLongClick(it, item)
                return@setOnLongClickListener true
            }
        }
    }
}