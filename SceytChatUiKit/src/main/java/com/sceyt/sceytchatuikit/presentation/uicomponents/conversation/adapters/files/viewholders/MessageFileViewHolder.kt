package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.data.models.messages.FileLoadData
import com.sceyt.sceytchatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import java.io.File

class MessageFileViewHolder(
        private val binding: SceytMessageFileItemBinding,
        private val messageListeners: MessageClickListenersImpl?
) : BaseFileViewHolder(binding.root) {

    init {
        binding.setupStyle()

        binding.root.setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem)
        }

        binding.root.setOnLongClickListener {
            messageListeners?.onAttachmentLongClick(it, fileItem)
            return@setOnLongClickListener true
        }
    }

    override fun bind(item: FileListItem) {
        binding.loadProgress.release(item.fileLoadData.progressPercent)
        super.bind(item)
        val file = (item as? FileListItem.File)?.file ?: return

        with(binding) {
            tvFileName.text = file.name

            if (item.message.incoming) {
                tvFileSize.text = file.fileSize.toPrettySize()
            } else {
                val size = if (file.fileSize == 0L) {
                    getFileSize(file.url)
                } else file.fileSize

                tvFileSize.text = size.toPrettySize()
            }
        }
    }

    private fun SceytMessageFileItemBinding.updateLoadState(data: FileLoadData) {
        loadProgress.isVisible = data.loading
        icFile.setImageResource(if (data.loading) 0 else MessagesStyle.fileAttachmentIcon)
        loadProgress.setProgress(data.progressPercent)
    }

    override fun updateUploadingState(data: FileLoadData) {
        binding.updateLoadState(data)
    }

    override fun updateDownloadingState(data: FileLoadData, file: File?) {
        binding.updateLoadState(data)
    }

    private fun SceytMessageFileItemBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
    }
}