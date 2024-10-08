package com.sceyt.chatuikit.presentation.components.media.adapter.holders

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytMediaItemVideoBinding
import com.sceyt.chatuikit.extensions.doSafe
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
import com.sceyt.chatuikit.presentation.helpers.ExoPlayerHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.media.OnMediaClickCallback
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import com.sceyt.chatuikit.presentation.components.media.applySystemWindowInsetsMargin

class MediaVideoViewHolder(private val binding: SceytMediaItemVideoBinding,
                           private val clickListeners: (MediaItem) -> Unit,
                           private val needMediaDataCallback: (NeedMediaInfoData) -> Unit)
    : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback) {
    private var playerHelper: ExoPlayerHelper? = null
    private var videoController: ConstraintLayout? = null
    private val mediaAdapter by lazy { bindingAdapter as? MediaAdapter }

    init {
        binding.root.setOnClickListener {
            clickListeners.invoke(fileItem)
        }

        binding.videoView.setOnClickListener {
            videoController?.isVisible = !(videoController?.isVisible ?: false)
            (context as? OnMediaClickCallback)?.onMediaClick()
        }
    }

    override fun bind(item: MediaItem) {
        super.bind(item)

        initVideoController()
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("ClickableViewAccessibility")
    private fun initVideoController() {
        doSafe { binding.videoView.controllerHideOnTouch = false }
        var isPlayingBeforePause = false
        binding.videoView.findViewById<ConstraintLayout>(R.id.videoTimeContainer)?.let { videoTimeContainer ->
            with(videoTimeContainer) {
                applySystemWindowInsetsMargin(applyBottom = true, userDefaultMargins = false)
                findViewById<DefaultTimeBar>(R.id.exo_progress)?.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isPlayingBeforePause = playerHelper?.isPlaying() ?: false
                            playerHelper?.pausePlayer()
                        }

                        MotionEvent.ACTION_UP -> if (isPlayingBeforePause) {
                            playerHelper?.resumePlayer()
                            initWakeLock()
                        }
                    }
                    false
                }
                isVisible = ((context as? MediaPreviewActivity)?.isShowMediaDetail() ?: true)
                videoController = this
            }
        }

        videoController?.findViewById<View>(R.id.exo_play_pause)?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && playerHelper?.player?.playbackState == Player.STATE_IDLE)
                initPlayerHelper(true)

            return@setOnTouchListener false
        }
    }

    private fun setPlayingState() {
        videoController?.applySystemWindowInsetsMargin(applyBottom = true, userDefaultMargins = false)
        videoController?.isVisible = ((context as? MediaPreviewActivity)?.isShowMediaDetail()
                ?: true)
        initPlayerHelper()
    }

    private fun initPlayer(): ExoPlayerHelper {
        binding.icThumb.isVisible = true
        return ExoPlayerHelper(context, binding.videoView, errorListener = {
            Toast.makeText(context, "Couldn't play video: ${it.message}", Toast.LENGTH_SHORT).show()
        }, listener = {
            if (it == ExoPlayerHelper.State.Ended)
                playerHelper?.seekToStart()

            if (it.isReady())
                binding.icThumb.isVisible = false
        }, playingListener = { playing ->
            if (playing) {
                initWakeLock()
            } else releaseWakeLock()
        }).also { mediaAdapter?.addMediaPlayer(it.player) }
    }

    private fun initWakeLock() {
        mediaAdapter?.initWakeLock(context)
    }

    private fun releaseWakeLock() {
        mediaAdapter?.releaseWakeLock()
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        setPlayingState()
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        playerHelper?.pausePlayer()
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        binding.progress.isVisible = data.state == Downloading || data.state == PendingDownload

        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawOriginalFile(binding.icThumb)
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }

            Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)

                binding.progress.setProgress(data.progressPercent)
            }

            Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawOriginalFile(binding.icThumb)
            }

            Downloaded, Uploaded -> {
                viewHolderHelper.drawOriginalFile(binding.icThumb)
                if (isAttachedToWindow)
                    initPlayerHelper()
            }

            PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }

            ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }

            FilePathChanged -> {
                viewHolderHelper.drawOriginalFile(binding.icThumb)
            }

            ThumbLoaded, Preparing, WaitingToUpload -> Unit
        }
    }

    private fun initPlayerHelper(playVideo: Boolean = shouldPlayVideo()) {
        if (!fileItem.file.filePath.isNullOrBlank()) {
            playerHelper = initPlayer()
            playerHelper?.setMediaPath(fileItem.file.filePath, playVideo)
            if (playVideo) initWakeLock()
        }
    }

    private fun shouldPlayVideo(): Boolean {
        return mediaAdapter?.shouldPlayVideoPath == fileItem.file.filePath
    }

    override fun needThumbFor() = ThumbFor.MediaPreview
}