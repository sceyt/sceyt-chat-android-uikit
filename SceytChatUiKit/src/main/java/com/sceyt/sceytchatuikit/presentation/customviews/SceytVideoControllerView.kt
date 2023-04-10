package com.sceyt.sceytchatuikit.presentation.customviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.material.imageview.ShapeableImageView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable


class SceytVideoControllerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private val playPauseItem: ImageView
    private var playDrawable: Drawable?
    private var pauseDrawable: Drawable?
    private var imageThumb: ShapeableImageView? = null
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
        setBackgroundColor(Color.TRANSPARENT)
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

        playPauseItem = ImageView(context).apply {
            background = context.getCompatDrawable(R.drawable.sceyt_bg_play_pause_button)
            layoutParams = LayoutParams(playPauseButtonSize, playPauseButtonSize).also {
                it.gravity = Gravity.CENTER
                setPadding(10, 10, 10, 10)
            }
            isVisible = showPlayPauseButton
        }
        playPauseItem.setImageDrawable(playDrawable)
        addView(playPauseItem)

        if (enablePlayPauseClick)
            setOnClickListeners()
    }

    private fun setOnClickListeners() {
        playPauseItem.setOnClickListener {
            if (playerView == null) return@setOnClickListener
            playerView?.isVisible = true

            if (isPlaying) {
                player?.pause()
                playPauseItem.setImageDrawable(playDrawable)
                onPlayPauseClick?.invoke(it, false)
            } else {
                if (!isInitializesPlayer)
                    initPlayer(mediaPath)
                if (isEnded)
                    player?.seekTo(0)
                player?.prepare()
                player?.play()
                playPauseItem.setImageDrawable(pauseDrawable)
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
                                imageThumb?.isVisible = true
                            }
                            Player.STATE_BUFFERING -> {}
                            Player.STATE_READY -> {
                                imageThumb?.isVisible = false
                                isEnded = false
                            }
                            Player.STATE_ENDED -> {
                                isEnded = true
                                isPlaying = false
                                playPauseItem.setImageDrawable(playDrawable)
                                imageThumb?.isVisible = true
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
        imageThumb?.isVisible = true
        playPauseItem.setImageDrawable(playDrawable)
    }

    private fun checkAndAddImage() {
        if (imageThumb == null) {
            imageThumb = ShapeableImageView(context).also {
                it.scaleType = ImageView.ScaleType.CENTER_CROP
                if (cornerSize > 0)
                    it.shapeAppearanceModel = it.shapeAppearanceModel.withCornerSize(cornerSize.toFloat())
            }
            addView(imageThumb, 0)
        }
    }

    fun setImageThumb(drawable: Drawable?) {
        checkAndAddImage()
        imageThumb?.setImageDrawable(drawable)
        imageThumb?.isVisible = true
    }

    fun getImageView(): ImageView {
        checkAndAddImage()
        return imageThumb!!
    }

    fun setBitmapImageThumb(bitmap: Bitmap?) {
        checkAndAddImage()
        imageThumb?.setImageBitmap(bitmap)
        imageThumb?.isVisible = true
    }

    fun setPlayerViewAndPath(playerView: PlayerView?, mediaPath: String?) {
        this.playerView = playerView
        this.mediaPath = mediaPath
    }

    fun showPlayPauseButtons(show: Boolean) {
        showPlayPauseButton = show
        playPauseItem.isVisible = show
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
        playPauseItem.setImageDrawable(playDrawable)
    }

    fun getPlayPauseImageView() = playPauseItem
}