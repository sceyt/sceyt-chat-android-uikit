package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders

import android.util.Size
import com.sceyt.chatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

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
        style.mediaLoaderStyle.apply(loadProgress)
    }
}