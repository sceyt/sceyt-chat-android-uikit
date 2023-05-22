package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.viewholders

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytMediaItemVideoBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.common.ExoPlayerHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.OnMediaClickCallback
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.SceytMediaActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.applySystemWindowInsetsMargin

class MediaVideoViewHolder(private val binding: SceytMediaItemVideoBinding,
                           private val clickListeners: (MediaItem) -> Unit,
                           private val needMediaDataCallback: (NeedMediaInfoData) -> Unit)
    : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback) {
    private val playerHelper by lazy { initPlayer() }
    private var videoController: ConstraintLayout? = null

    init {
        binding.root.setOnClickListener {
            clickListeners.invoke(fileItem)
        }
    }

    override fun bind(item: MediaItem) {
        super.bind(item)

        setListener()
        initVideoController()

        viewHolderHelper.transferData?.let {
            updateState(it, true)
            binding.progress.release(it.progressPercent)

            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        binding.icThumb.isVisible = true
        (bindingAdapter as? MediaAdapter)?.addMediaPlayer(binding.videoView.player)

        binding.videoView.setOnClickListener {
            videoController?.isVisible = !(videoController?.isVisible ?: false)
            (context as? OnMediaClickCallback)?.onMediaClick()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initVideoController() {
        @UnstableApi
        binding.videoView.controllerHideOnTouch = false
        var isPlayingBeforePause = playerHelper.isPlaying()
        with(binding.videoView.findViewById<ConstraintLayout>(R.id.videoTimeContainer)) {
            applySystemWindowInsetsMargin(applyBottom = true, userDefaultMargins = false)
            findViewById<DefaultTimeBar>(R.id.exo_progress)?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPlayingBeforePause = playerHelper.isPlaying()
                        playerHelper.pausePlayer()
                    }
                    MotionEvent.ACTION_UP -> if (isPlayingBeforePause) playerHelper.resumePlayer()
                }
                false
            }
            isVisible = ((context as? SceytMediaActivity)?.isShowMediaDetail() ?: true)
            videoController = this
        }
    }

    private fun setPlayingState(isVisibleToUser: Boolean) {
        if (!isVisibleToUser && playerHelper.isPlaying())
            playerHelper.pausePlayer()

        if (isVisibleToUser)
            videoController?.applySystemWindowInsetsMargin(applyBottom = true, userDefaultMargins = false)

        videoController?.isVisible = ((context as? SceytMediaActivity)?.isShowMediaDetail() ?: true)
    }

    private fun initPlayer(): ExoPlayerHelper {
        return ExoPlayerHelper(context, binding.videoView, errorListener = {
            Toast.makeText(context, "Couldn't play video: ${it.message}", Toast.LENGTH_SHORT).show()
        }, listener = {
            if (it == ExoPlayerHelper.State.Ended)
                playerHelper.seekToStart()

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
                playerHelper.setMediaPath(data.filePath, shouldPlayVideo())
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

    private fun shouldPlayVideo(): Boolean {
        return (bindingAdapter as MediaAdapter).shouldPlayVideoPath == fileItem.file.filePath
    }

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }

    override fun needThumbFor() = ThumbFor.MediaPreview
}