package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.FileLoadData
import com.sceyt.chat.ui.databinding.SceytMessageFileItemBinding
import com.sceyt.chat.ui.extensions.getFileSize
import com.sceyt.chat.ui.extensions.toPrettySize
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import java.io.File

class MessageFileViewHolder(
        private val binding: SceytMessageFileItemBinding,
        private val messageListeners: MessageClickListenersImpl?
) : BaseFileViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem)
        }

        binding.root.setOnLongClickListener {
            messageListeners?.onAttachmentLongClick(it, fileItem)
            return@setOnLongClickListener true
        }
    }

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
            }
        }
    }

    private fun SceytMessageFileItemBinding.updateLoadState(data: FileLoadData) {
        loadProgress.isVisible = data.loading
        icFile.setImageResource(if (data.loading) 0 else R.drawable.sceyt_ic_file)
        loadProgress.progress = data.progressPercent.toInt()
    }

    override fun updateUploadingState(data: FileLoadData) {
        binding.updateLoadState(data)
    }

    override fun updateDownloadingState(data: FileLoadData, file: File?) {
        binding.updateLoadState(data)
    }
}