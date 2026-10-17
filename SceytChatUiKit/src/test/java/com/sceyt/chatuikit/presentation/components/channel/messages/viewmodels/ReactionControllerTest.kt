package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.presentation.components.channel.messages.events.ReactionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReactionControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val reactionInteractor = mock<MessageReactionInteractor>()
    private val channelId = 7L
    private val notifications = mutableListOf<Pair<SceytResponse<*>, Boolean>>()

    private fun CoroutineScope.controller() = ReactionController(
        scope = this,
        reactionInteractor = reactionInteractor,
        channelId = { channelId },
        notifyResponse = { response, showError -> notifications.add(response to showError) },
        ioDispatcher = dispatcher,
    )

    private fun msg(id: Long) = createMessage(createdAt = id, id = id, tid = id)

    @Test
    fun `add reaction event delegates to interactor and reports state without error`() =
        runTest(dispatcher) {
            whenever { reactionInteractor.addReaction(any(), any(), any(), any(), any(), any()) }
                .thenReturn(SceytResponse.Success(null))
            val controller = controller()

            controller.onEvent(ReactionEvent.AddReaction(msg(1), "like"))
            advanceUntilIdle()

            verifyBlocking(reactionInteractor) {
                addReaction(eq(channelId), eq(1L), eq("like"), eq(1), eq(""), eq(false))
            }
            assertThat(notifications).hasSize(1)
            assertThat(notifications.single().second).isFalse()
        }

    @Test
    fun `remove reaction event delegates to delete`() = runTest(dispatcher) {
        whenever { reactionInteractor.deleteReaction(any(), any(), any()) }
            .thenReturn(SceytResponse.Success(null))
        val controller = controller()

        controller.onEvent(ReactionEvent.RemoveReaction(msg(2), "like"))
        advanceUntilIdle()

        verifyBlocking(reactionInteractor) { deleteReaction(eq(channelId), eq(2L), eq("like")) }
        assertThat(notifications.single().second).isFalse()
    }

    @Test
    fun `direct add forwards score, reason and enforceUnique`() = runTest(dispatcher) {
        whenever { reactionInteractor.addReaction(any(), any(), any(), any(), any(), any()) }
            .thenReturn(SceytResponse.Success(null))
        val controller = controller()

        controller.add(msg(3), scoreKey = "fire", score = 5, reason = "why", enforceUnique = true)
        advanceUntilIdle()

        verifyBlocking(reactionInteractor, times(1)) {
            addReaction(eq(channelId), eq(3L), eq("fire"), eq(5), eq("why"), eq(true))
        }
    }
}