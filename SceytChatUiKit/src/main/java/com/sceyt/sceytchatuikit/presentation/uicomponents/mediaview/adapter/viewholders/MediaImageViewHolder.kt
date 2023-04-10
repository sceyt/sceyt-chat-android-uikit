package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytMediaItemImageBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem

class MediaImageViewHolder(private val binding: SceytMediaItemImageBinding,
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
                viewHolderHelper.drawOriginalFile(binding.imageView)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawOriginalFile(binding.imageView)
            }
            TransferState.Uploaded -> {
                viewHolderHelper.drawOriginalFile(binding.imageView)
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
                viewHolderHelper.drawOriginalFile(binding.imageView)
            }
            TransferState.PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }
            TransferState.ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }
            TransferState.FilePathChanged -> {
                viewHolderHelper.drawOriginalFile(binding.imageView)
            }
            TransferState.ThumbLoaded -> Unit
        }
    }

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }
}