package com.sceyt.chatuikit.presentation.components.media.adapter.holders

import android.widget.Toast
import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytMediaItemVideoBinding
import com.sceyt.chatuikit.extensions.forceApplySystemWindowInsetsPadding
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.media.OnMediaClickCallback
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaAdapter
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.presentation.helpers.ExoPlayerHelper
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.preview.MediaPreviewStyle

class MediaVideoViewHolder(
    private val binding: SceytMediaItemVideoBinding,
    private val style: MediaPreviewStyle,
    private val clickListeners: (MediaItem) -> Unit,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val loaderClickListener: (MediaItem) -> Unit = {},
) : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback),
    SharedTransitionViewProvider {
    private var playerHelper: ExoPlayerHelper? = null
    private val mediaAdapter by lazy { bindingAdapter as? MediaAdapter }
    private var pendingReadyCallback: (() -> Unit)? = null
    private var isOriginalImageReady = false
    private var preparedFilePath: String? = null

    init {
        binding.applyStyle()

        binding.root.setOnClickListener {
            clickListeners.invoke(fileItem)
        }

        binding.progress.setOnClickListener {
            loaderClickListener.invoke(fileItem)
        }

        binding.videoView.setOnClickListener {
            with(binding.videoController) {
                setControllerVisible(!isControllerVisible, animate = true)
            }
            (context as? OnMediaClickCallback)?.onMediaClick()
        }

        binding.videoController.forceApplySystemWindowInsetsPadding(
            applyBottom = true,
            applyRight = true,
            applyLeft = true
        )
    }

    override fun bind(item: MediaItem) {
        super.bind(item)
        syncControllerVisibility()
        pendingReadyCallback = null
        isOriginalImageReady = false
    }

    private fun syncControllerVisibility() {
        val visible = (context as? MediaPreviewActivity)?.isVisibleToolbar() ?: true
        binding.videoController.setControllerVisible(visible, animate = false)
    }

    private fun setPlayingState() {
        syncControllerVisibility()
        initPlayerHelper()
    }

    private fun initPlayer(): ExoPlayerHelper {
        showThumb()
        return ExoPlayerHelper(context, binding.videoView, errorListener = {
            Toast.makeText(context, "Couldn't play video: ${it.message}", Toast.LENGTH_SHORT).show()
        }, listener = {
            if (it == ExoPlayerHelper.State.Ended)
                playerHelper?.seekToStart()

            if (it.isReady())
                fadeOutThumb()
        }, playingListener = { playing ->
            if (playing) {
                initWakeLock()
            } else releaseWakeLock()
        }).also {
            mediaAdapter?.addMediaPlayer(it.player)
            binding.videoController.setPlayer(it.player)
        }
    }

    private fun showThumb() {
        with(binding.icThumb) {
            animate().cancel()
            alpha = 1f
            isVisible = true
        }
    }

    private fun fadeOutThumb() {
        with(binding.icThumb) {
            if (!isVisible) return
            animate().alpha(0f).setDuration(THUMB_FADE_DURATION_MS).withEndAction {
                isVisible = false
                alpha = 1f
            }.start()
        }
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
        releaseWakeLock()
        binding.videoController.setPlayer(null)
        playerHelper?.let { helper ->
            mediaAdapter?.savePlaybackPosition(fileItem.attachment.filePath, helper.player.currentPosition)
            mediaAdapter?.removeMediaPlayer(helper.player)
            helper.releasePlayer()
        }
        playerHelper = null
        preparedFilePath = null
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)

        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                drawOriginalFile()
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
                viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }

            Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.icThumb)
            }

            Uploading -> {
                if (isOnBind)
                    drawOriginalFile()
            }

            Downloaded, Uploaded -> {
                drawOriginalFile()
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
                drawOriginalFile()
            }

            ThumbLoaded, Preparing, WaitingToUpload -> Unit
        }
    }

    private fun initPlayerHelper(playVideo: Boolean = shouldPlayVideo()) {
        val filePath = fileItem.attachment.filePath
        if (filePath.isNullOrBlank()) return

        val existingHelper = playerHelper
        if (existingHelper != null && preparedFilePath == filePath) {
            if (playVideo) {
                existingHelper.resumePlayer()
                initWakeLock()
            }
            return
        }

        existingHelper?.let { helper ->
            mediaAdapter?.removeMediaPlayer(helper.player)
            helper.releasePlayer()
        }

        playerHelper = initPlayer()
        preparedFilePath = filePath
        playerHelper?.setMediaPath(
            url = filePath,
            playVideo = playVideo,
            startPositionMs = mediaAdapter?.getPlaybackPosition(filePath) ?: 0L
        )
        if (playVideo) initWakeLock()
    }

    private fun shouldPlayVideo(): Boolean {
        return mediaAdapter?.shouldPlayVideoPath == fileItem.attachment.filePath
    }

    override fun needThumbFor() = ThumbFor.MediaPreview

    override val loadingProgressViewWithStyle: Pair<CircularProgressView, MediaLoaderStyle>
        get() = binding.progress to style.mediaLoaderStyle

    override fun provide() = binding.icThumb

    override fun awaitReadyForSharedTransition(onReady: () -> Unit) {
        if (isOriginalImageReady) {
            onReady()
        } else {
            pendingReadyCallback = onReady
        }
    }

    private fun drawOriginalFile() {
        val filePath = fileItem.attachment.filePath
        isOriginalImageReady = false
        viewHolderHelper.drawOriginalFile(binding.icThumb) ready@{
            if (filePath.isNullOrBlank() || filePath != fileItem.attachment.filePath) {
                return@ready
            }
            isOriginalImageReady = true
            pendingReadyCallback?.invoke()
            pendingReadyCallback = null
        }
    }

    private fun SceytMediaItemVideoBinding.applyStyle() {
        style.mediaLoaderStyle.apply(progress)
        videoController.applyStyle(
            backgroundColor = style.videoControllerBackgroundColor,
            playIcon = style.playIcon,
            pauseIcon = style.pauseIcon,
            trackColor = style.trackColor,
            progressColor = style.progressColor,
            thumbColor = style.thumbColor,
            timelineTextStyle = style.timelineTextStyle
        )
    }

    private companion object {
        const val THUMB_FADE_DURATION_MS = 150L
    }
}