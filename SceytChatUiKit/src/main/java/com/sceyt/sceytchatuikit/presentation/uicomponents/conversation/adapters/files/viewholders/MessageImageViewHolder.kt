package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import com.sceyt.sceytchatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder<FileListItem>(binding.root, needMediaDataCallback) {

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
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (!viewHolderHelper.updateTransferData(data, fileItem, ::isValidThumb)) return

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }

            Uploading, Preparing, WaitingToUpload -> {
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
                else viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, binding.fileImage)
            }

            PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }

            ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank())
                    requestThumb()
            }

            ThumbLoaded -> {
                if (isValidThumb(data.thumbData))
                    viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, binding.fileImage)
            }
        }
    }

    override fun getThumbSize() = Size(1080, 1080)

    override fun needThumbFor() = ThumbFor.ConversationInfo

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }

    private fun SceytMessageImageItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}