package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.util.Size
import com.sceyt.sceytchatuikit.databinding.ItemChannelImageBinding
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl

class ImageViewHolder(private val binding: ItemChannelImageBinding,
                      private val clickListeners: AttachmentClickListenersImpl,
                      private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        setListener()

        transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank())
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()
    }


    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data

        when (data.state) {
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.Uploaded -> {
                drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.PendingDownload -> {
                loadBlurThumb(blurredThumb, binding.fileImage)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
            }
            TransferState.Downloading -> {
                if (isOnBind)
                    loadBlurThumb(blurredThumb, binding.fileImage)
            }
            TransferState.Downloaded -> {
                drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.PauseDownload -> {
                loadBlurThumb(blurredThumb, binding.fileImage)
            }
            TransferState.ErrorDownload -> {
                loadBlurThumb(blurredThumb, binding.fileImage)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                loadThumb(data.filePath, binding.fileImage)
            }
        }
    }

    private fun requestThumb() {
        itemView.post {
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem, getThumbSize()))
        }
    }

    override fun getThumbSize() = Size(itemView.width, itemView.height)

    private fun setListener() {
        TransferUpdateObserver.setListener(listenerKey, ::updateState)
    }
}