package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import com.sceyt.chatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.customviews.CircularProgressView
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle

class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseMessageFileViewHolder<FileListItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()

        binding.root.setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem, message)
        }

        binding.root.setOnLongClickListener {
            messageListeners?.onAttachmentLongClick(it, fileItem, message)
            return@setOnLongClickListener true
        }

        binding.loadProgress.setOnClickListener {
            messageListeners?.onAttachmentLoaderClick(it, fileItem, message)
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
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

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    override fun needThumbFor() = ThumbFor.MessagesLisView

    private fun SceytMessageImageItemBinding.applyStyle() {
        loadProgress.setProgressColor(style.mediaLoaderColor)
    }
}