package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class ExoPlayerHelper(private val context: Context,
                      private val playerView: PlayerView,
                      private val errorListener: ((PlaybackException) -> Unit)? = null,
                      private val listener: PlayerStateChangeCallback? = null) : Player.Listener {

    private lateinit var exoPlayer: ExoPlayer

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(context).build()
        player.repeatMode = Player.REPEAT_MODE_OFF
        playerView.player = exoPlayer
        exoPlayer.addListener(this)
    }


    fun setMediaPath(url: String?, playVideo: Boolean) {
        url?.let {
            exoPlayer.addMediaItem(MediaItem.fromUri(it))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = playVideo
        }
    }

    fun pausePlayer() {
        exoPlayer.playWhenReady = false
        exoPlayer.playbackState
    }

    fun resumePlayer() {
        exoPlayer.playWhenReady = true
        exoPlayer.playbackState
    }

    fun retryPlayer() {
        exoPlayer.prepare()
        exoPlayer.playbackState
    }

    fun releasePlayer() {
        exoPlayer.release()
    }

    fun restartVideo() {
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = true
    }

    fun isPlaying(): Boolean {
        return exoPlayer.isPlaying
    }

    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_BUFFERING -> listener?.updateState(State.Buffering)
            Player.STATE_ENDED -> listener?.updateState(State.Ended)
            Player.STATE_IDLE -> listener?.updateState(State.Idle)
            Player.STATE_READY -> listener?.updateState(State.Ready)
            else -> listener?.updateState(State.Unknown)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        errorListener?.invoke(error)
    }

    val videoDuration: Int
        get() = exoPlayer.duration.toInt()

    fun seekToStart() {
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = false
    }

    val player: ExoPlayer
        get() = exoPlayer

    fun interface PlayerStateChangeCallback {
        fun updateState(state: State)
    }

    enum class State {
        Buffering,
        Ended,
        Idle,
        Ready,
        Unknown;

        fun isPlaying(): Boolean {
            return this == Ready
        }
    }
}