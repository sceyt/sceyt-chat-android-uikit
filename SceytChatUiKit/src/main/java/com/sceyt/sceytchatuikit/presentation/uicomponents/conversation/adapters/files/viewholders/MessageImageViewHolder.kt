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
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder(binding.root) {

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

        binding.loadProgress.release(item.file.progressPercent)
        transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            Uploading -> {
                if (isOnBind)
                    drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            Uploaded -> {
                drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            PendingDownload -> {
                loadBlurThumb(blurredThumb, binding.fileImage)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
            }
            Downloading -> {
                if (isOnBind)
                    loadBlurThumb(blurredThumb, binding.fileImage)
            }
            Downloaded -> {
                drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            PauseDownload -> {
                loadBlurThumb(blurredThumb, binding.fileImage)
            }
            ErrorDownload -> {
                loadBlurThumb(blurredThumb, binding.fileImage)
            }
            FilePathChanged -> {
                requestThumb()
            }
            ThumbLoaded -> {
                loadThumb(data.filePath, binding.fileImage)
            }
        }
    }

    private fun requestThumb() {
        itemView.post {
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem, getThumbSize()))
        }
    }

    override fun getThumbSize() = Size(binding.fileImage.width, binding.fileImage.height)

    private fun setListener() {
        TransferUpdateObserver.setListener(listenerKey, ::updateState)
    }

    private fun SceytMessageImageItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}