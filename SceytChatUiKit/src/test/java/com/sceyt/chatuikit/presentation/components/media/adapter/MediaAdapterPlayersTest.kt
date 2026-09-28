package com.sceyt.chatuikit.presentation.components.media.adapter

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/**
 * Tests the player bookkeeping the media preview relies on.
 *
 * Each video view holder owns exactly one player: it registers the player it creates and
 * unregisters it before replacing it or when the item is detached. Releasing a player that was
 * already unregistered would release it twice, and a player left registered after being released
 * would be released again when the screen is destroyed.
 */
@RunWith(RobolectricTestRunner::class)
class MediaAdapterPlayersTest {

    private fun createAdapter() = MediaAdapter(
        attachmentViewHolderFactory = mock<MediaFilesViewHolderFactory>(),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun `releaseAllPlayers releases every registered player`() {
        val adapter = createAdapter()
        val firstPlayer = mock<Player>()
        val secondPlayer = mock<Player>()
        adapter.addMediaPlayer(firstPlayer)
        adapter.addMediaPlayer(secondPlayer)

        adapter.releaseAllPlayers()

        verify(firstPlayer).release()
        verify(secondPlayer).release()
    }

    @Test
    fun `a player unregistered on detach is not released again`() {
        val adapter = createAdapter()
        val detachedPlayer = mock<Player>()
        val attachedPlayer = mock<Player>()
        adapter.addMediaPlayer(detachedPlayer)
        adapter.addMediaPlayer(attachedPlayer)

        // The view holder released this one itself when the item was detached.
        adapter.removeMediaPlayer(detachedPlayer)
        adapter.releaseAllPlayers()

        verify(detachedPlayer, never()).release()
        verify(attachedPlayer).release()
    }

    @Test
    fun `releaseAllPlayers clears the registry so a second call releases nothing`() {
        val adapter = createAdapter()
        val player = mock<Player>()
        adapter.addMediaPlayer(player)

        adapter.releaseAllPlayers()
        adapter.releaseAllPlayers()

        verify(player).release()
    }

    @Test
    fun `pauseAllVideos pauses registered players without releasing them`() {
        val adapter = createAdapter()
        val player = mock<Player>()
        adapter.addMediaPlayer(player)

        adapter.pauseAllVideos()

        verify(player).pause()
        verify(player, never()).release()
    }

    @Test
    fun `playback positions are kept per file and dropped at the start of a video`() {
        val adapter = createAdapter()

        adapter.savePlaybackPosition("/videos/first.mp4", 5_000L)
        adapter.savePlaybackPosition("/videos/second.mp4", 1_200L)

        assert(adapter.getPlaybackPosition("/videos/first.mp4") == 5_000L)
        assert(adapter.getPlaybackPosition("/videos/second.mp4") == 1_200L)
        assert(adapter.getPlaybackPosition("/videos/unknown.mp4") == 0L)

        // Restarting from the beginning must not leave a stale resume position behind.
        adapter.savePlaybackPosition("/videos/first.mp4", 0L)
        assert(adapter.getPlaybackPosition("/videos/first.mp4") == 0L)
    }
}
