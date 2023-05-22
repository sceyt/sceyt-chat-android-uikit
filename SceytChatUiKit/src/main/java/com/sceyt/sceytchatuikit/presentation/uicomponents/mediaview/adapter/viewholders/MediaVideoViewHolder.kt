package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders

import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytMediaItemVideoBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.common.ExoPlayerHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.OnMediaClickCallback
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.applySystemWindowInsetsMargin

class MediaVideoViewHolder(private val binding: SceytMediaItemVideoBinding,
                           private val clickListeners: (MediaItem) -> Unit,
                           private val needMediaDataCallback: (NeedMediaInfoData) -> Unit)
    : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback) {
    private val playerHelper by lazy { initPlayer() }

    init {
        binding.root.setOnClickListener {
            clickListeners.invoke(fileItem)
        }
    }

    override fun bind(item: MediaItem) {
        super.bind(item)

        setListener()
        val controller = initVideoController()

        viewHolderHelper.transferData?.let {
            updateState(it, true)
            binding.progress.release(it.progressPercent)

            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        binding.icThumb.isVisible = true
        (bindingAdapter as? MediaAdapter)?.addMediaPlayer(binding.videoView.player)

        binding.videoView.setOnClickListener {
            controller.isVisible = !controller.isVisible
            (context as? OnMediaClickCallback)?.onMediaClick()
        }
    }

    private fun initVideoController(): ConstraintLayout {
        @UnstableApi
        binding.videoView.controllerHideOnTouch = false
        val controller = binding.videoView.findViewById<ConstraintLayout>(R.id.videoTimeContainer)
        controller?.applySystemWindowInsetsMargin(applyBottom = true, userDefaultMargins = false)
        return controller
    }

    private fun setPlayingState(isVisibleToUser: Boolean) {
        if (!isVisibleToUser) {
            if (playerHelper.isPlaying()) {
                playerHelper.pausePlayer()
            }
        } else {
            if (fileItem.file.filePath.isNotNullOrBlank()) {
                playerHelper.restartVideo()
            }
        }
    }

    private fun initPlayer(): ExoPlayerHelper {
        return ExoPlayerHelper(context, binding.videoView, errorListener = {
            Toast.makeText(context, "Couldn't play video: ${it.message}", Toast.LENGTH_SHORT).show()
        }, listener = {
            if (it == ExoPlayerHelper.State.Ended) {
                binding.videoView.player?.seekTo(0)
                binding.videoView.player?.playWhenReady = false
            }
            if (it.isPlaying())
                binding.icThumb.isVisible = false
        })
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
                viewHolderHelper.drawOriginalFile(binding.icThumb)
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
                    viewHolderHelper.drawOriginalFile(binding.icThumb)
            }

            TransferState.Downloaded, TransferState.Uploaded -> {
                viewHolderHelper.drawOriginalFile(binding.icThumb)
                playerHelper.setMediaPath(data.filePath, itemView.hasWindowFocus())
            }

            TransferState.PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }

            TransferState.ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }

            TransferState.FilePathChanged -> {
                viewHolderHelper.drawOriginalFile(binding.icThumb)
            }

            TransferState.ThumbLoaded -> Unit
        }
    }

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }

    override fun needThumbFor() = ThumbFor.MediaPreview
}