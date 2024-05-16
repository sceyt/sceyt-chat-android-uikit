package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytVideoControllerViewBinding
import com.sceyt.chatuikit.extensions.getCompatDrawable


class SceytVideoControllerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private var binding: SceytVideoControllerViewBinding
    private var playDrawable: Drawable?
    private var pauseDrawable: Drawable?
    private var isPlaying = false
    private var isEnded = false
    private var isInitializesPlayer = false
    private var mediaPath: String? = null
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var onPlayPauseClick: ((view: View, play: Boolean) -> Unit)? = null
    private var showPlayPauseButton = true
    private var playPauseButtonSize = 130
    private var cornerSize = 0

    init {
        binding = SceytVideoControllerViewBinding.inflate(LayoutInflater.from(context), this, true)

        val a = context.obtainStyledAttributes(attrs, R.styleable.SceytVideoControllerView)
        showPlayPauseButton = a.getBoolean(R.styleable.SceytVideoControllerView_sceytVideoControllerShowPlayPause, showPlayPauseButton)
        playPauseButtonSize = a.getDimensionPixelSize(R.styleable.SceytVideoControllerView_sceytVideoControllerPlayPauseSize, playPauseButtonSize)
        playDrawable = a.getDrawable(R.styleable.SceytVideoControllerView_sceytVideoControllerPauseIcon)
                ?: context.getCompatDrawable(R.drawable.sceyt_ic_play)
        pauseDrawable = a.getDrawable(R.styleable.SceytVideoControllerView_sceytVideoControllerPauseIcon)
                ?: context.getCompatDrawable(R.drawable.sceyt_ic_pause)
        cornerSize = a.getDimensionPixelSize(R.styleable.SceytVideoControllerView_sceytVideoControllerCornerSize, cornerSize)
        a.getDrawable(R.styleable.SceytVideoControllerView_sceytVideoControllerImage)?.let {
            setImageThumb(it)
        }
        val enablePlayPauseClick = a.getBoolean(R.styleable.SceytVideoControllerView_sceytVideoControllerEnablePlayPauseClick, true)
        a.recycle()

        binding.applyStyle()

        if (enablePlayPauseClick)
            setOnClickListeners()
    }

    private fun SceytVideoControllerViewBinding.applyStyle() {
        playPauseItem.apply {
            layoutParams.width = playPauseButtonSize
            layoutParams.height = playPauseButtonSize
            playPauseItem.setImageDrawable(playDrawable)
            isVisible = showPlayPauseButton
        }
        imageThumb.apply {
            shapeAppearanceModel = shapeAppearanceModel.withCornerSize(cornerSize.toFloat())
        }
    }

    private fun setOnClickListeners() {
        binding.playPauseItem.setOnClickListener {
            if (playerView == null) return@setOnClickListener
            playerView?.isVisible = true

            if (isPlaying) {
                player?.pause()
                binding.playPauseItem.setImageDrawable(playDrawable)
                onPlayPauseClick?.invoke(it, false)
            } else {
                if (!isInitializesPlayer)
                    initPlayer(mediaPath)
                if (isEnded)
                    player?.seekTo(0)
                player?.prepare()
                player?.play()
                binding.playPauseItem.setImageDrawable(pauseDrawable)
                onPlayPauseClick?.invoke(it, true)
            }
            isPlaying = !isPlaying
        }
    }

    private fun initPlayer(mediaPath: String?) {
        if (playerView == null) return
        ExoPlayer.Builder(context)
            .build()
            .also {
                player = it
                playerView?.player = player
                setMediaItem(it, mediaPath)

                it.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        setInitialState()
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        super.onPlaybackStateChanged(state)
                        when (state) {
                            Player.STATE_IDLE -> {
                                isEnded = false
                                binding.imageThumb.isVisible = true
                            }

                            Player.STATE_BUFFERING -> {}
                            Player.STATE_READY -> {
                                binding.imageThumb.isVisible = false
                                isEnded = false
                            }

                            Player.STATE_ENDED -> {
                                isEnded = true
                                isPlaying = false
                                binding.playPauseItem.setImageDrawable(playDrawable)
                                binding.imageThumb.isVisible = true
                            }
                        }
                    }
                })
                isInitializesPlayer = true
            }
    }

    private fun setMediaItem(player: Player, path: String?) {
        if (path == null) return
        val mediaItem = MediaItem.fromUri(path)
        player.setMediaItem(mediaItem)
    }

    private fun setInitialState() {
        isPlaying = false
        binding.imageThumb.isVisible = true
        binding.playPauseItem.setImageDrawable(playDrawable)
    }

    fun setImageThumb(drawable: Drawable?) {
        binding.imageThumb.setImageDrawable(drawable)
        binding.imageThumb.isVisible = true
    }

    fun getImageView(): ImageView {
        return binding.imageThumb
    }

    fun setBitmapImageThumb(bitmap: Bitmap?) {
        binding.imageThumb.setImageBitmap(bitmap)
        binding.imageThumb.isVisible = true
    }

    fun setPlayerViewAndPath(playerView: PlayerView?, mediaPath: String?) {
        this.playerView = playerView
        this.mediaPath = mediaPath
    }

    fun showPlayPauseButtons(show: Boolean) {
        showPlayPauseButton = show
        binding.playPauseItem.isVisible = show
    }

    fun setPlayPauseClickListener(listener: (view: View, play: Boolean) -> Unit) {
        onPlayPauseClick = listener
    }

    fun release() {
        isInitializesPlayer = false
        player?.release()
        player = null
        setInitialState()
    }

    fun pause() {
        player?.pause()
        binding.playPauseItem.setImageDrawable(playDrawable)
    }

    fun getPlayPauseImageView() = binding.playPauseItem
}