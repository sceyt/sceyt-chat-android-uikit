package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
        private val needDownloadCallback: (FileListItem) -> Unit) : BaseFileViewHolder(binding.root) {

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

        setListener()

        binding.loadProgress.release()
        transferData?.let {
            updateState(it, true)
            if (it.filePath == null)
                needDownloadCallback.invoke(fileItem)
        }
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                loadImage(fileItem.file.filePath, binding.fileImage)
            }
            PendingDownload -> {
                loadThumb(thumb, binding.fileImage)
                needDownloadCallback.invoke(fileItem)
            }
            Downloading -> {
                if (isOnBind)
                    loadThumb(thumb, binding.fileImage)
            }
            Uploading -> {
                if (isOnBind)
                    loadImage(fileItem.file.filePath, binding.fileImage)
            }
            Uploaded, Downloaded -> {
                loadImage(fileItem.file.filePath, binding.fileImage)
            }
            PauseDownload -> {
                loadThumb(thumb, binding.fileImage)
            }
            ErrorDownload -> {
                loadThumb(thumb, binding.fileImage)
            }
        }
    }

    private fun setListener() {
        MessageFilesAdapter.setListener(listenerKey, ::updateState)
    }

    private fun SceytMessageImageItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}