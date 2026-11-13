package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sceyt.chatuikit.extensions.doSafe
import com.sceyt.chatuikit.extensions.toMediaUri

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
            private set

        internal fun clearLastPlayerIfOwned(player: Player) {
            if (lastPlayer === player) lastPlayer = null
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        // Pause only: the previous player belongs to another view holder or screen, which may
        // still be using it and is responsible for releasing it.
        doSafe { lastPlayer?.pause() }
        val appContext = context.applicationContext
        exoPlayer = ExoPlayer.Builder(
            appContext,
            DefaultRenderersFactory(appContext).setEnableDecoderFallback(true)
        ).build().also {
            lastPlayer = it
        }
        player.repeatMode = Player.REPEAT_MODE_OFF
        playerView.player = exoPlayer
        exoPlayer.addListener(this)
    }

    fun setMediaPath(url: String?, playVideo: Boolean, startPositionMs: Long = 0) {
        if (isSetMediaPath) return
        url?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it.toMediaUri()))
            exoPlayer.prepare()
            if (startPositionMs > 0) exoPlayer.seekTo(startPositionMs)
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
        playerView.player = null
        exoPlayer.release()
        clearLastPlayerIfOwned(exoPlayer)
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

    override fun onTracksChanged(tracks: Tracks) {
        if (exoPlayer.videoSize != VideoSize.UNKNOWN) return
        tracks.singleSelectedVideoFormat()?.let(::applyVideoAspectRatio)
    }

    private fun Tracks.singleSelectedVideoFormat(): Format? {
        val selectedFormats = groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .flatMap { group ->
                (0 until group.length)
                    .filter(group::isTrackSelected)
                    .map(group::getTrackFormat)
            }
        return selectedFormats.singleOrNull()
    }

    @OptIn(UnstableApi::class)
    private fun applyVideoAspectRatio(format: Format) {
        if (format.width <= 0 || format.height <= 0) return
        val aspectRatio = format.width * format.pixelWidthHeightRatio / format.height
        val isRotated = format.rotationDegrees == 90 || format.rotationDegrees == 270
        val displayAspectRatio = if (isRotated) 1 / aspectRatio else aspectRatio
        val contentFrame = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_content_frame)
        (contentFrame as? AspectRatioFrameLayout)?.setAspectRatio(displayAspectRatio)
    }

    val videoDuration: Int
        get() = exoPlayer.duration
            .takeIf { it != C.TIME_UNSET }
            ?.coerceIn(0L, Int.MAX_VALUE.toLong())
            ?.toInt() ?: 0

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
