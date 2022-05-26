package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.getCompatDrawable


class VideoControllerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private val playPauseItem: ImageView
    private var playDrawable: Drawable?
    private var pauseDrawable: Drawable?
    private var imageThumb: AppCompatImageView? = null
    private var isPlaying = false
    private var isEnded = false
    private var isInitializesPlayer = false
    private var mediaPath: String? = null
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var onPlayPauseClick: ((play: Boolean) -> Unit)? = null
    private var showPlayPauseButton = true

    init {
        setBackgroundColor(Color.TRANSPARENT)
        playDrawable = context.getCompatDrawable(R.drawable.ic_play)
        pauseDrawable = context.getCompatDrawable(R.drawable.ic_pause)

        val a = context.obtainStyledAttributes(attrs, R.styleable.VideoControllerView)
        showPlayPauseButton = a.getBoolean(R.styleable.VideoControllerView_videoControllerShowPlayPause, false)
        a.recycle()

        playPauseItem = ImageView(context).apply {
            background = context.getCompatDrawable(R.drawable.bg_play_pause_button)
            layoutParams = LayoutParams(130, 130).also {
                it.gravity = Gravity.CENTER
                setPadding(10, 10, 10, 10)
            }
            isVisible = showPlayPauseButton
        }
        playPauseItem.setImageDrawable(playDrawable)
        addView(playPauseItem)
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        playPauseItem.setOnClickListener {
            if (playerView == null) return@setOnClickListener
            playerView?.isVisible = true

            if (isPlaying) {
                player?.pause()
                playPauseItem.setImageDrawable(playDrawable)
                onPlayPauseClick?.invoke(false)
            } else {
                if (!isInitializesPlayer)
                    initPlayer(mediaPath)
                if (isEnded)
                    player?.seekTo(0)
                player?.prepare()
                player?.play()
                playPauseItem.setImageDrawable(pauseDrawable)
                onPlayPauseClick?.invoke(true)
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

    fun setImageThumb(drawable: Drawable) {
        if (imageThumb == null) {
            imageThumb = AppCompatImageView(context).also {
                it.setImageDrawable(drawable)
                it.scaleType = ImageView.ScaleType.CENTER_CROP
            }
            addView(imageThumb, 0)
        } else imageThumb?.setImageDrawable(drawable)
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

    fun setPlayPauseClickListener(listener: (Boolean) -> Unit) {
        onPlayPauseClick = listener
    }

    fun release() {
        isInitializesPlayer = false
        player?.release()
        player = null
        setInitialState()
    }
}