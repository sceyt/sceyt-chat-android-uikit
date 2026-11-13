package com.sceyt.chatuikit.presentation.helpers

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ExoPlayerHelperTest {
    private val context = RuntimeEnvironment.getApplication()
    private val helpers = mutableListOf<ExoPlayerHelper>()

    @After
    fun tearDown() {
        helpers.forEach { it.releasePlayer() }
    }

    @Test
    fun `set media path prepares the first path only`() {
        val helper = createHelper()

        helper.setMediaPath(FIRST_PATH, playVideo = false)
        helper.setMediaPath(SECOND_PATH, playVideo = true)

        assertThat(helper.player.mediaItemCount).isEqualTo(1)
        assertThat(helper.player.currentMediaItem?.localConfiguration?.uri)
            .isEqualTo(Uri.fromFile(File(FIRST_PATH)))
        assertThat(helper.player.playWhenReady).isFalse()
    }

    @Test
    fun `set media path seeks to start position and applies play flag`() {
        val helper = createHelper()

        helper.setMediaPath(FIRST_PATH, playVideo = true, startPositionMs = 1_500)

        assertThat(helper.player.currentPosition).isEqualTo(1_500)
        assertThat(helper.player.playWhenReady).isTrue()
    }

    @Test
    fun `null media path is ignored and a later path is still prepared`() {
        val helper = createHelper()

        helper.setMediaPath(null, playVideo = true)
        assertThat(helper.player.mediaItemCount).isEqualTo(0)

        helper.setMediaPath(FIRST_PATH, playVideo = false)
        assertThat(helper.player.mediaItemCount).isEqualTo(1)
    }

    @Test
    fun `player is attached to player view with repeat off`() {
        val playerView = PlayerView(context)
        val helper = createHelper(playerView = playerView)

        assertThat(playerView.player).isSameInstanceAs(helper.player)
        assertThat(helper.player.repeatMode).isEqualTo(Player.REPEAT_MODE_OFF)
    }

    @Test
    fun `resume and pause toggle play when ready`() {
        val helper = createHelper()

        helper.resumePlayer()
        assertThat(helper.player.playWhenReady).isTrue()

        helper.pausePlayer()
        assertThat(helper.player.playWhenReady).isFalse()
    }

    @Test
    fun `retry player prepares an idle player again`() {
        val helper = createHelper()
        helper.player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(FIRST_PATH))))
        assertThat(helper.player.playbackState).isEqualTo(Player.STATE_IDLE)

        helper.retryPlayer()

        assertThat(helper.player.playbackState).isEqualTo(Player.STATE_BUFFERING)
    }

    @Test
    fun `restart video seeks to beginning and plays`() {
        val helper = createHelper()
        helper.setMediaPath(FIRST_PATH, playVideo = false, startPositionMs = 1_500)

        helper.restartVideo()

        assertThat(helper.player.currentPosition).isEqualTo(0)
        assertThat(helper.player.playWhenReady).isTrue()
    }

    @Test
    fun `seek to start seeks to beginning and pauses`() {
        val helper = createHelper()
        helper.setMediaPath(FIRST_PATH, playVideo = true, startPositionMs = 1_500)

        helper.seekToStart()

        assertThat(helper.player.currentPosition).isEqualTo(0)
        assertThat(helper.player.playWhenReady).isFalse()
    }

    @Test
    fun `is playing is false until media is ready`() {
        val helper = createHelper()

        helper.setMediaPath(FIRST_PATH, playVideo = true)

        assertThat(helper.isPlaying()).isFalse()
    }

    @Test
    fun `video duration is zero while duration is unknown`() {
        val helper = createHelper()

        helper.setMediaPath(FIRST_PATH, playVideo = false)

        assertThat(helper.videoDuration).isEqualTo(0)
    }

    @Test
    fun `only ready state reports ready`() {
        val readyStates = ExoPlayerHelper.State.entries.filter { it.isReady() }

        assertThat(readyStates).containsExactly(ExoPlayerHelper.State.Ready)
    }

    @Test
    fun `new helper pauses the previous player and becomes last player`() {
        val first = createHelper()
        first.resumePlayer()

        val second = createHelper()

        assertThat(first.player.playWhenReady).isFalse()
        assertThat(ExoPlayerHelper.lastPlayer).isSameInstanceAs(second.player)
    }

    @Test
    fun `releasing an older helper keeps the newer last player`() {
        val first = createHelper()
        val second = createHelper()

        first.releasePlayer()
        assertThat(ExoPlayerHelper.lastPlayer).isSameInstanceAs(second.player)

        second.releasePlayer()
        assertThat(ExoPlayerHelper.lastPlayer).isNull()
    }

    @Test
    fun `release player detaches player view`() {
        val playerView = PlayerView(context)
        val helper = createHelper(playerView = playerView)
        assertThat(playerView.player).isSameInstanceAs(helper.player)

        helper.releasePlayer()

        assertThat(playerView.player).isNull()
    }

    @Test
    fun `playback states are reported to state callback`() {
        val states = mutableListOf<ExoPlayerHelper.State>()
        val helper = createHelper(listener = { states.add(it) })

        helper.onPlaybackStateChanged(Player.STATE_BUFFERING)
        helper.onPlaybackStateChanged(Player.STATE_READY)
        helper.onPlaybackStateChanged(Player.STATE_ENDED)
        helper.onPlaybackStateChanged(Player.STATE_IDLE)
        helper.onPlaybackStateChanged(UNKNOWN_STATE)

        assertThat(states).containsExactly(
            ExoPlayerHelper.State.Buffering,
            ExoPlayerHelper.State.Ready,
            ExoPlayerHelper.State.Ended,
            ExoPlayerHelper.State.Idle,
            ExoPlayerHelper.State.Unknown
        ).inOrder()
    }

    @Test
    fun `playing changes and errors are reported to listeners`() {
        val playingStates = mutableListOf<Boolean>()
        val errors = mutableListOf<PlaybackException>()
        val helper = createHelper(
            errorListener = { errors.add(it) },
            playingListener = { playingStates.add(it) }
        )
        val error = PlaybackException("failed", null, PlaybackException.ERROR_CODE_UNSPECIFIED)

        helper.onIsPlayingChanged(true)
        helper.onIsPlayingChanged(false)
        helper.onPlayerError(error)

        assertThat(playingStates).containsExactly(true, false).inOrder()
        assertThat(errors).containsExactly(error)
    }

    private fun createHelper(
        playerView: PlayerView = PlayerView(context),
        errorListener: ((PlaybackException) -> Unit)? = null,
        listener: ExoPlayerHelper.PlayerStateChangeCallback? = null,
        playingListener: ((Boolean) -> Unit)? = null,
    ): ExoPlayerHelper {
        return ExoPlayerHelper(context, playerView, errorListener, listener, playingListener)
            .also { helpers.add(it) }
    }

    private companion object {
        const val FIRST_PATH = "/videos/first.mp4"
        const val SECOND_PATH = "/videos/second.mp4"
        const val UNKNOWN_STATE = 99
    }
}
