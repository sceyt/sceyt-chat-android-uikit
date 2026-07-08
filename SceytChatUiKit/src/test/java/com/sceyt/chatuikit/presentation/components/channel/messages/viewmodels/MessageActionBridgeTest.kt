package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessageActionBridgeTest {

    private fun message(id: Long) = createMessage(createdAt = id, id = id, tid = id)

    @Test
    fun `showMessageActions emits one semantic action effect`() = runTest {
        val bridge = MessageActionBridge()
        val effects = mutableListOf<MessageActionBridge.Effect>()
        val job: Job = launch { bridge.effects.collect { effects.add(it) } }
        advanceUntilIdle()

        bridge.showMessageActions(message(1), message(2))
        advanceUntilIdle()
        job.cancel()

        val effect = effects.single() as MessageActionBridge.Effect.MessageActionsShown
        assertThat(effect.messages.map { it.tid }).containsExactly(1L, 2L).inOrder()
    }

    @Test
    fun `cancelMultiSelectMode emits one semantic cancel effect`() = runTest {
        val bridge = MessageActionBridge()
        val effects = mutableListOf<MessageActionBridge.Effect>()
        val job: Job = launch { bridge.effects.collect { effects.add(it) } }
        advanceUntilIdle()

        bridge.cancelMultiSelectMode()
        advanceUntilIdle()
        job.cancel()

        assertThat(effects).containsExactly(MessageActionBridge.Effect.MultiSelectCanceled)
    }

    @Test
    fun `hideMessageActions emits one semantic hide effect`() = runTest {
        val bridge = MessageActionBridge()
        val effects = mutableListOf<MessageActionBridge.Effect>()
        val job: Job = launch { bridge.effects.collect { effects.add(it) } }
        advanceUntilIdle()

        bridge.hideMessageActions()
        advanceUntilIdle()
        job.cancel()

        assertThat(effects).containsExactly(MessageActionBridge.Effect.MessageActionsHidden)
    }
}
