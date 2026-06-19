package com.sceyt.chatuikit.media.audio

import android.view.View
import com.google.common.truth.Truth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AudioPlayerStateCollectorTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun lifecycleDestroy_cancelsSubscriptionWithoutDetach() {
        val state = MutableStateFlow(AudioPlayerState())
        val received = mutableListOf<AudioPlayerState>()
        val lifecycleOwner = TestLifecycleOwner()
        val view = View(RuntimeEnvironment.getApplication())
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        val collector = AudioPlayerStateCollector(state, received::add)
        lifecycleOwner.moveTo(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.moveTo(Lifecycle.Event.ON_START)

        collector.start(view)
        assertThat(state.subscriptionCount.value).isEqualTo(1)

        lifecycleOwner.moveTo(Lifecycle.Event.ON_DESTROY)

        assertThat(state.subscriptionCount.value).isEqualTo(0)
        state.value = playingState(FILE_A, TID_A)
        assertThat(received).containsExactly(AudioPlayerState())
    }

    @Test
    fun collector_doesNotSubscribeUntilAttached() {
        val state = MutableStateFlow(AudioPlayerState())
        val received = mutableListOf<AudioPlayerState>()
        val collector = AudioPlayerStateCollector(state, received::add)

        state.value = playingState(FILE_A, TID_A)

        assertThat(state.subscriptionCount.value).isEqualTo(0)
        assertThat(received).isEmpty()

        collector.start()

        assertThat(state.subscriptionCount.value).isEqualTo(1)
        assertThat(received).containsExactly(playingState(FILE_A, TID_A))
        collector.stop()
    }

    @Test
    fun startAndStop_collectOnlyWhileAttached() {
        val state = MutableStateFlow(AudioPlayerState())
        val received = mutableListOf<AudioPlayerState>()
        val collector = AudioPlayerStateCollector(state, received::add)

        collector.start()
        collector.start()
        assertThat(state.subscriptionCount.value).isEqualTo(1)
        state.value = playingState(FILE_A, TID_A)
        collector.stop()
        assertThat(state.subscriptionCount.value).isEqualTo(0)
        state.value = playingState(FILE_B, TID_B)

        assertThat(received.map { it.filePath }).containsExactly(null, FILE_A).inOrder()
    }

    @Test
    fun restart_reconnectsToLatestPlaybackState() {
        val state = MutableStateFlow(AudioPlayerState())
        val received = mutableListOf<AudioPlayerState>()
        val collector = AudioPlayerStateCollector(state, received::add)

        collector.start()
        collector.stop()
        state.value = playingState(FILE_A, TID_A)
        collector.start()

        assertThat(received.last()).isEqualTo(playingState(FILE_A, TID_A))
        collector.stop()
    }

    @Test
    fun multipleSurfaces_receiveSameStateIndependently() {
        val state = MutableStateFlow(AudioPlayerState())
        val messageStates = mutableListOf<AudioPlayerState>()
        val infoStates = mutableListOf<AudioPlayerState>()
        val messageCollector = AudioPlayerStateCollector(state, messageStates::add)
        val infoCollector = AudioPlayerStateCollector(state, infoStates::add)

        messageCollector.start()
        infoCollector.start()
        assertThat(state.subscriptionCount.value).isEqualTo(2)
        state.value = playingState(FILE_A, TID_A)
        infoCollector.stop()
        assertThat(state.subscriptionCount.value).isEqualTo(1)
        state.value = playingState(FILE_B, TID_B)

        assertThat(messageStates.last()).isEqualTo(playingState(FILE_B, TID_B))
        assertThat(infoStates.last()).isEqualTo(playingState(FILE_A, TID_A))
        messageCollector.stop()
        assertThat(state.subscriptionCount.value).isEqualTo(0)
    }

    private fun playingState(filePath: String, messageTid: MessageTid) = AudioPlayerState(
        filePath = filePath,
        messageTid = messageTid,
        status = AudioPlayerStatus.Playing,
        duration = 1_000
    )

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry

        fun moveTo(event: Lifecycle.Event) {
            registry.handleLifecycleEvent(event)
        }
    }

    private companion object {
        const val FILE_A = "/voice/a.aac"
        const val FILE_B = "/voice/b.aac"
        const val TID_A = 10L
        const val TID_B = 20L
    }
}
