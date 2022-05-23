package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import com.sceyt.chat.ui.databinding.SceytUiMessageFileItemBinding
import com.sceyt.chat.ui.extensions.toPrettySize
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.utils.FileCompressorUtil

class MessageFileViewHolder(
        private val binding: SceytUiMessageFileItemBinding
) : BaseFileViewHolder(binding.root) {

    init {
        with(binding.root) {
            setOnCreateContextMenuListener { menu, v, menuInfo ->
                return@setOnCreateContextMenuListener
            }
        }
    }

    override fun bindTo(item: FileListItem) {
        val fileItem = (item as? FileListItem.File)?.file ?: return

        binding.apply {
            loadData = item.fileLoadData
            tvFileName.text = fileItem.name

            if (item.message.incoming) {
                tvFileSize.text = fileItem.uploadedFileSize.toPrettySize()
            } else {
                val size = if (fileItem.uploadedFileSize == 0L) {
                    FileCompressorUtil.getFileSize(fileItem.url)
                } else fileItem.uploadedFileSize

                tvFileSize.text = size.toPrettySize()
            }

            setUploadListenerIfNeeded(item)
            downloadIfNeeded(item)

            root.setOnClickListener {
                openFile(item, itemView.context)
            }
        }
    }
}