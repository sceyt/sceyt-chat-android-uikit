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

    private lateinit var fileItem: FileListItem

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
        fileItem = item

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

    override fun updateUploadingState(load: FileLoadData, finish: Boolean) {
        binding.updateLoadState(load, finish)
    }

    override fun updateDownloadingState(load: FileLoadData) {
        binding.updateLoadState(load, false)
    }

    override fun downloadFinish(load: FileLoadData, file: File?) {
        binding.updateLoadState(load, true)
    }

    private fun SceytMessageFileItemBinding.updateLoadState(data: FileLoadData, finish: Boolean) {
        if (finish) {
            loadProgress.isVisible = false
            icFile.setImageResource(R.drawable.sceyt_ic_file)
        } else {
            loadProgress.isVisible = data.loading
            if (data.loading) {
                icFile.setImageResource(0)
                loadProgress.progress = data.progressPercent
            } else
                icFile.setImageResource(R.drawable.sceyt_ic_file)
        }
    }
}