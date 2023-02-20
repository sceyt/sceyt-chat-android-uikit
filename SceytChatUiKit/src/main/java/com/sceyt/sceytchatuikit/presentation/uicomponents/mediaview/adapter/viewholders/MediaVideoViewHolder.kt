package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.databinding.SceytMediaItemVideoBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.MediaActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem

class MediaVideoViewHolder(private val binding: SceytMediaItemVideoBinding,
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
            binding.progress.release(it.progressPercent)

            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        binding.icThumb.isVisible = true
        binding.videoView.setPlayingListener { playing ->
            if (playing)
                binding.icThumb.isVisible = false
        }

        (bindingAdapter as? MediaAdapter)?.addMediaPlayer(binding.videoView.mediaPlayer)

        binding.root.post {
            if (fileItem.file.filePath.isNotNullOrBlank())
                binding.videoView.start()
        }
    }

    private fun setPlayingState(isVisibleToUser: Boolean) {
        if (!isVisibleToUser) {
            if (binding.videoView.isPlaying) {
                binding.videoView.pause()
            }
            binding.videoView.setUserVisibleHint(false)
        } else
            binding.videoView.setUserVisibleHint((context as? MediaActivity)?.isShowMediaDetail()
                    ?: false)
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

        binding.progress.isVisible = data.state == TransferState.Downloading

        when (data.state) {
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(binding.icThumb, ::requestThumb)
            }
            TransferState.PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }
            TransferState.Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)

                binding.progress.setProgress(data.progressPercent)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(binding.icThumb, ::requestThumb)
            }
            TransferState.Downloaded, TransferState.Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.icThumb, ::requestThumb)
                binding.videoView.setVideoPath(mediaPath = data.filePath, startPlay = itemView.hasWindowFocus(), isLooping = true)
            }
            TransferState.PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }
            TransferState.ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                viewHolderHelper.loadThumb(data.filePath, binding.icThumb)
            }
        }
    }

    override fun getThumbSize() = Size(binding.root.width, binding.root.height)

    private fun setListener() {
        MessageEventsObserver.onTransferUpdatedLiveData
            .observe(context.asComponentActivity(), ::updateState)
    }
}