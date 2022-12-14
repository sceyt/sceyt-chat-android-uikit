package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.toPrettySize
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
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        val file = (item as? FileListItem.File)?.file ?: return

        with(binding) {
            tvFileName.text = file.name

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

        setListener()
        /* val transferData = item.file.fileTransferData

         if (transferData == null) {
             //  Log.i("sdfsdf", "filePath ${item.file.filePath}  url ${item.file.url}")
             if (item.file.filePath.isNullOrBlank() && item.file.url.isNotNullOrBlank()) {
                 //  Log.i("sdfsdf", "needDownloadCallback")

                 binding.loadProgress.isVisible = true
                 needDownloadCallback.invoke(item)
             }
             return
         }*/

        item.file.transferState?.let {
            updateState(it, item.file.progressPercent ?: 0f)
        }
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        if (isFileItemInitialized)
            setListener()
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        /*if (isFileItemInitialized)
            fileItem.file.removeListener()*/
    }


    private fun updateState(state: TransferState, progressPercent: Float) {
        //Log.i("sdfsdf22", "$transferData  $isFileItemInitialized")

        if (isFileItemInitialized.not()) return
        fileItem.file.transferState = state
        fileItem.file.progressPercent = progressPercent
        // fileItem.file.fileTransferData = transferData
        binding.loadProgress.getProgressWithState(state, progressPercent)
        when (state) {
            TransferState.PendingUpload -> {
                //  binding.loadProgress.release()
                binding.icFile.setImageResource(0)
                // binding.loadProgress.isVisible = true
            }
            TransferState.PendingDownload -> {
                needDownloadCallback.invoke(fileItem)
            }
            TransferState.Downloading -> {
                binding.icFile.setImageResource(0)
                // binding.loadProgress.isVisible = true
                // binding.loadProgress.setProgress(transferData.progressPercent)
            }
            TransferState.Uploading -> {
                binding.icFile.setImageResource(0)
                ////binding.loadProgress.isVisible = true
                //binding.loadProgress.setProgress(transferData.progressPercent)
            }
            TransferState.Uploaded -> {
                //binding.loadProgress.isVisible = false
                binding.icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
            }
            TransferState.Downloaded -> {
                //binding.loadProgress.isVisible = false
                binding.icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
            }
            TransferState.ErrorDownload -> {
                /*   binding.loadProgress.apply {
                       isVisible = true
                       setTransferring(false)
                   }*/
                //binding.loadProgress.isVisible = false
            }
            TransferState.ErrorUpload -> {
                /*  binding.loadProgress.apply {
                      isVisible = true
                      setTransferring(false)
                  }*/
                // binding.loadProgress.isVisible = false
            }
        }
    }

    private fun setListener() {
        MessageFilesAdapter.setListener(listenerKey) {
            updateState(it.state, it.progressPercent)
        }
    }

    private fun SceytMessageFileItemBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
        loadProgress.setBackgroundColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}