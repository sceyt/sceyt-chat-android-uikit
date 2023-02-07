package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder(binding.root, needMediaDataCallback) {

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

        setListener()

        binding.loadProgress.release(item.file.progressPercent)
        viewHolderHelper.transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (viewHolderHelper.isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        viewHolderHelper.transferData = data

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }
            Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }
            Downloaded -> {
                if (fileItem.thumbPath.isNullOrBlank())
                    viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
                else viewHolderHelper.loadThumb(fileItem.thumbPath, binding.fileImage)
            }
            PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }
            ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }
            FilePathChanged -> {
                requestThumb()
            }
            ThumbLoaded -> {
                viewHolderHelper.loadThumb(data.filePath, binding.fileImage)
            }
        }
    }

    override fun getThumbSize() = Size(1080, 1080)

    private fun setListener() {
        TransferUpdateObserver.setListener(viewHolderHelper.listenerKey, ::updateState)
    }

    private fun SceytMessageImageItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}