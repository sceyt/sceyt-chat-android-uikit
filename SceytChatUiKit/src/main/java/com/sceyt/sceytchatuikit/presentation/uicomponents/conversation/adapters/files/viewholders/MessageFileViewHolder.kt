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
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class MessageFileViewHolder(
        private val binding: SceytMessageFileItemBinding,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseFileViewHolder(binding.root, needMediaDataCallback) {

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
        val file = (item as? FileListItem.File)?.file ?: return
        setListener()

        with(binding) {
            tvFileName.text = file.name
            loadProgress.release(file.progressPercent)
            tvFileSize.text = file.fileSize.toPrettySize()
        }

        viewHolderHelper.transferData?.let {
            updateState(it)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }
    }

    private fun updateState(data: TransferData) {
        if (!viewHolderHelper.updateTransferData(data, fileItem)) return

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload, PauseUpload -> {
                binding.icFile.setImageResource(0)
            }
            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
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
        TransferUpdateObserver.setListener(viewHolderHelper.listenerKey, ::updateState)
    }

    private fun SceytMessageFileItemBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
        loadProgress.setBackgroundColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}