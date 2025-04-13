package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class ExoPlayerHelper(
        private val context: Context,
        private val playerView: PlayerView,
        private val errorListener: ((PlaybackException) -> Unit)? = null,
        private val listener: PlayerStateChangeCallback? = null,
        private val playingListener: ((Boolean) -> Unit)? = null
) : Player.Listener {

    private lateinit var exoPlayer: ExoPlayer
    private var isSetMediaPath = false

    init {
        initializePlayer()
    }

    companion object {
        var lastPlayer: ExoPlayer? = null
    }

    private fun initializePlayer() {
        lastPlayer?.stop()
        lastPlayer?.release()
        exoPlayer = ExoPlayer.Builder(context).build().also {
            lastPlayer = it
        }
        player.repeatMode = Player.REPEAT_MODE_OFF
        playerView.player = exoPlayer
        exoPlayer.addListener(this)
    }

    fun setMediaPath(url: String?, playVideo: Boolean) {
        if (isSetMediaPath) return
        url?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = playVideo
            isSetMediaPath = true
        }
    }

    fun pausePlayer() {
        exoPlayer.playWhenReady = false
    }

    fun resumePlayer() {
        exoPlayer.playWhenReady = true
    }

    fun retryPlayer() {
        exoPlayer.prepare()
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

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        playingListener?.invoke(isPlaying)
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

        fun isReady(): Boolean {
            return this == Ready
        }
    }
}