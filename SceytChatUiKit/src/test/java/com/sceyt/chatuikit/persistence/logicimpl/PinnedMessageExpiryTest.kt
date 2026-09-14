package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.logicimpl.usecases.sceytPinnedMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinnedMessageExpiryTest {
    @Test
    fun `pins expire without another database emission`() = runTest {
        val pins = MutableStateFlow(listOf(
            sceytPinnedMessage(id = 1L, pinnedUntil = 1_000L),
            sceytPinnedMessage(id = 2L)
        ))
        val emissions = mutableListOf<List<Long>>()
        pins.observeUnexpiredPins { testScheduler.currentTime }
            .onEach { emissions.add(it.map { pin -> pin.id }) }
            .launchIn(backgroundScope)

        runCurrent()
        assertThat(emissions.last()).containsExactly(1L, 2L)
        advanceTimeBy(1_000L)
        runCurrent()
        assertThat(emissions.last()).containsExactly(2L)
    }

    @Test
    fun `a refreshed expiry replaces the previous timer`() = runTest {
        val pins = MutableStateFlow(listOf(sceytPinnedMessage(pinnedUntil = 1_000L)))
        val emissions = mutableListOf<List<Long>>()
        pins.observeUnexpiredPins { testScheduler.currentTime }
            .onEach { emissions.add(it.map { pin -> pin.id }) }
            .launchIn(backgroundScope)
        runCurrent()
        pins.value = listOf(sceytPinnedMessage(pinnedUntil = 2_000L))
        runCurrent()
        advanceTimeBy(1_000L)
        runCurrent()
        assertThat(emissions.last()).containsExactly(500L)
        advanceTimeBy(1_000L)
        runCurrent()
        assertThat(emissions.last()).isEmpty()
    }
}
