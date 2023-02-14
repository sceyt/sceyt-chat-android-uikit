package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders

import android.util.Size
import com.sceyt.sceytchatuikit.databinding.ItemImageBinding
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem

class MediaImageViewHolder(private val binding: ItemImageBinding,
                           private val clickListeners: (MediaItem) -> Unit,
                           private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback) {

    init {
        binding.imageView.setOnClickListener {
            clickListeners.invoke(fileItem)
        }
    }

    override fun bind(item: MediaItem) {
        super.bind(item)
        setListener()

        viewHolderHelper.transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        if (fileItem.thumbPath.isNullOrBlank() && !fileItem.file.filePath.isNullOrBlank())
            requestThumb()
    }


    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (!viewHolderHelper.updateTransferData(data, fileItem)) return

        when (data.state) {
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(binding.imageView, ::requestThumb)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(binding.imageView, ::requestThumb)
            }
            TransferState.Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.imageView, ::requestThumb)
            }
            TransferState.PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }
            TransferState.Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }
            TransferState.Downloaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.imageView, ::requestThumb)
                (fileItem as MediaItem.Image).file.filePath = data.filePath
            }
            TransferState.PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }
            TransferState.ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                viewHolderHelper.loadThumb(fileItem.thumbPath, binding.imageView)
            }
        }
    }

    override fun getThumbSize() = Size(itemView.width, itemView.height)

    private fun setListener() {
        TransferUpdateObserver.setListener(viewHolderHelper.listenerKey, ::updateState)
    }
}