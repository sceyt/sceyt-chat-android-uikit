package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders

import android.util.Size
import com.sceyt.sceytchatuikit.databinding.ItemVideoBinding
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.MediaActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem

class MediaVideoViewHolder(private val binding: ItemVideoBinding,
                           private val clickListeners: (MediaItem) -> Unit,
                           private val needMediaDataCallback: (NeedMediaInfoData) -> Unit)
    : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback) {

    init {
        binding.root.setOnClickListener {
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
    }

    private fun setPlayingState(isVisibleToUser: Boolean) {
        if (!isVisibleToUser) {
            if (binding.videoView.isPlaying) {
                binding.videoView.pause()
            }
            binding.videoView.setUserVisibleHint(false)
        } else {
            binding.videoView.setUserVisibleHint((context as MediaActivity).isShowMediaDetail())
        }
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        setPlayingState(true)
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        setPlayingState(false)
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (!viewHolderHelper.updateTransferData(data, fileItem)) return

        when (data.state) {
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                // viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)
            }
            TransferState.PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                //viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }
            TransferState.Downloading -> {
                /*if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.image)*/
            }
            TransferState.Uploading -> {
                /* if (isOnBind)
                     viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)*/
            }
            TransferState.Downloaded, TransferState.Uploaded -> {
                //  viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)
                binding.videoView.setVideoPath(mediaPath = data.filePath, startPlay = true, isLooping = true)
            }
            TransferState.PauseDownload -> {
                // viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }
            TransferState.ErrorDownload -> {
                // viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                //  viewHolderHelper.loadThumb(data.filePath, binding.image)
            }
        }
    }

    override fun getThumbSize() = Size(binding.root.width, binding.root.height)

    private fun setListener() {
        TransferUpdateObserver.setListener(viewHolderHelper.listenerKey, ::updateState)
    }
}