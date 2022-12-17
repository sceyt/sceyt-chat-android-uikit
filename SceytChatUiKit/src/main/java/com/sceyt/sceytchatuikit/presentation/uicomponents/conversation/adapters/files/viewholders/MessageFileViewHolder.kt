package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class MessageFileViewHolder(
        private val binding: SceytMessageFileItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
        private val needDownloadCallback: (FileListItem) -> Unit
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

        binding.loadProgress.setOnClickListener {
            messageListeners?.onAttachmentLoaderClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        val file = (item as? FileListItem.File)?.file ?: return
        setListener()

        with(binding) {
            tvFileName.text = file.name
            loadProgress.release()

            if (item.message.incoming) {
                tvFileSize.text = file.fileSize.toPrettySize()
            } else {
                val size = if (file.fileSize == 0L) {
                    file.filePath?.let {
                        getFileSize(it).also { size -> file.fileSize = size }
                    } ?: 0L
                } else file.fileSize

                tvFileSize.text = size.toPrettySize()
            }
        }

        transferData?.let {
            updateState(it)
            if (it.state == TransferState.Downloading)
                needDownloadCallback.invoke(fileItem)
        }
    }

    private fun updateState(data: TransferData) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data
        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            TransferState.PendingUpload -> {
                binding.icFile.setImageResource(0)
            }
            TransferState.PendingDownload -> {
                needDownloadCallback.invoke(fileItem)
            }
            TransferState.Downloading, TransferState.Uploading -> {
                binding.icFile.setImageResource(0)
            }
            TransferState.Uploaded, TransferState.Downloaded -> {
                binding.icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
            }
        }
    }

    private fun setListener() {
        MessageFilesAdapter.setListener(listenerKey, ::updateState)
    }

    private fun SceytMessageFileItemBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
        loadProgress.setBackgroundColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}