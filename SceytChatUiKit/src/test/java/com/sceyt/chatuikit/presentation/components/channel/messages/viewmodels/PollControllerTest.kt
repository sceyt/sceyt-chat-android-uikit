package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.persistence.interactor.MessagePollInteractor
import com.sceyt.chatuikit.presentation.components.channel.messages.events.PollEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PollControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val pollInteractor = mock<MessagePollInteractor>()
    private val channelId = 7L
    private val notifications = mutableListOf<Pair<SceytResponse<*>, Boolean>>()

    private fun CoroutineScope.controller() = PollController(
        scope = this,
        pollInteractor = pollInteractor,
        channelId = { channelId },
        notifyResponse = { response, showError -> notifications.add(response to showError) },
    )

    private fun pollMessage(
        pollId: String = "p1",
        allowRetract: Boolean = true,
        hasOwnVotes: Boolean = true,
        closed: Boolean = false,
    ): SceytMessage {
        val poll = mock<SceytPollDetails> {
            on { id } doReturn pollId
            on { allowVoteRetract } doReturn allowRetract
            on { ownVotes } doReturn if (hasOwnVotes) listOf(mock()) else emptyList()
            on { this.closed } doReturn closed
        }
        return createMessage(createdAt = 1, id = 1, tid = 1).copy(poll = poll)
    }

    @Test
    fun `toggle vote delegates to interactor without error flag`() = runTest(dispatcher) {
        whenever { pollInteractor.toggleVote(any(), any(), any(), any()) }
            .thenReturn(SceytResponse.Success(null))
        val option = mock<PollOption> { on { id } doReturn "o1" }
        val controller = controller()

        controller.onEvent(PollEvent.ToggleVote(pollMessage(), option))
        advanceUntilIdle()

        verifyBlocking(pollInteractor) { toggleVote(eq(channelId), eq(1L), eq("p1"), eq("o1")) }
        assertThat(notifications.single().second).isFalse()
    }

    @Test
    fun `retract is ignored when retract is not allowed`() = runTest(dispatcher) {
        val controller = controller()

        controller.onEvent(PollEvent.RetractVote(pollMessage(allowRetract = false)))
        advanceUntilIdle()

        verifyBlocking(pollInteractor, never()) { retractVote(any(), any(), any()) }
    }

    @Test
    fun `retract is ignored when there are no own votes`() = runTest(dispatcher) {
        val controller = controller()

        controller.onEvent(PollEvent.RetractVote(pollMessage(hasOwnVotes = false)))
        advanceUntilIdle()

        verifyBlocking(pollInteractor, never()) { retractVote(any(), any(), any()) }
    }

    @Test
    fun `retract proceeds and reports state with error flag`() = runTest(dispatcher) {
        whenever { pollInteractor.retractVote(any(), any(), any()) }
            .thenReturn(SceytResponse.Success(null))
        val controller = controller()

        controller.onEvent(PollEvent.RetractVote(pollMessage()))
        advanceUntilIdle()

        verifyBlocking(pollInteractor) { retractVote(eq(channelId), eq(1L), eq("p1")) }
        assertThat(notifications.single().second).isTrue()
    }

    @Test
    fun `end is ignored when poll already closed`() = runTest(dispatcher) {
        val controller = controller()

        controller.onEvent(PollEvent.EndVote(pollMessage(closed = true)))
        advanceUntilIdle()

        verifyBlocking(pollInteractor, never()) { endPoll(any(), any(), any()) }
    }

    @Test
    fun `end proceeds and reports state with error flag`() = runTest(dispatcher) {
        whenever { pollInteractor.endPoll(any(), any(), any()) }
            .thenReturn(SceytResponse.Success(null))
        val controller = controller()

        controller.onEvent(PollEvent.EndVote(pollMessage()))
        advanceUntilIdle()

        verifyBlocking(pollInteractor) { endPoll(eq(channelId), eq(1L), eq("p1")) }
        assertThat(notifications.single().second).isTrue()
    }
}