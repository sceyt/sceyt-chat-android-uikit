package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class MessageFileViewHolder(
        private val binding: SceytMessageFileItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
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
            loadProgress.release(file.progressPercent)
            tvFileSize.text = file.fileSize.toPrettySize()
        }

        transferData?.let {
            updateState(it)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
        }
    }

    private fun updateState(data: TransferData) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data
        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload, PauseUpload -> {
                binding.icFile.setImageResource(0)
            }
            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
            }
            Downloading, Uploading -> {
                binding.icFile.setImageResource(0)
            }
            Uploaded, Downloaded -> {
                binding.icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
            }
            ErrorUpload, ErrorDownload, PauseDownload -> {
                binding.icFile.setImageResource(0)
            }
            FilePathChanged, ThumbLoaded -> return
        }
    }

    private fun setListener() {
        TransferUpdateObserver.setListener(listenerKey, ::updateState)
    }

    private fun SceytMessageFileItemBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
        loadProgress.setBackgroundColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}