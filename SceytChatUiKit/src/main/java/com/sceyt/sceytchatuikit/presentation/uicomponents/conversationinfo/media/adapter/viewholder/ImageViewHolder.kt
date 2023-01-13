package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.util.Log
import android.util.Size
import com.sceyt.sceytchatuikit.databinding.ItemChannelImageBinding
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseChannelFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl

class ImageViewHolder(private val binding: ItemChannelImageBinding,
                      private val clickListeners: AttachmentClickListenersImpl,
                      private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseChannelFileViewHolder(binding.root, needMediaDataCallback) {

    init {
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        setListener()

        viewHolderHelper.transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()
    }


    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (viewHolderHelper.isFileItemInitialized.not() || (data.messageTid != fileItem.file.messageTid)) return
        viewHolderHelper.transferData = data
        fileItem.file.updateWithTransferData(data)

        when (data.state) {
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }
            TransferState.Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }
            TransferState.Downloaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.fileImage, ::requestThumb)
            }
            TransferState.PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }
            TransferState.ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.fileImage)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                viewHolderHelper.loadThumb(data.filePath, binding.fileImage)
            }
        }
    }

    override fun getThumbSize() = Size(itemView.width, itemView.height)

    private fun setListener() {
        TransferUpdateObserver.setListener(viewHolderHelper.listenerKey, ::updateState)
    }
}