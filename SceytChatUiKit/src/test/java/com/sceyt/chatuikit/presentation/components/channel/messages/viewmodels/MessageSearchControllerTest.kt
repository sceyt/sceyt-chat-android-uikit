package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class MessageSearchControllerTest {
    private val messageInteractor = mock<MessageInteractor>()
    private val preparingToScroll = AtomicBoolean(false)
    private val scrolledTo = mutableListOf<Long>()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun CoroutineScope.controller(queryLimit: Int = 10) = MessageSearchController(
        scope = this,
        messageInteractor = messageInteractor,
        conversationId = { 1L },
        replyInThread = false,
        isPreparingToScrollToMessage = preparingToScroll,
        messageListQueryLimit = { queryLimit },
        onScrollToSearchMessage = { scrolledTo.add(it.id) },
    )

    private fun stubSearch(vararg ids: Long, hasNext: Boolean = false) {
        whenever { messageInteractor.searchMessages(any(), any(), any()) }
            .thenReturn(SceytPagingResponse.Success(ids.map { msg(it) }, hasNext = hasNext))
    }

    private fun msg(id: Long): SceytMessage = createMessage(createdAt = id, id = id, tid = id)

    @Test
    fun `search sorts results, resets index and scrolls to first`() = runTest {
        stubSearch(3, 1, 2)
        val controller = controller()

        controller.search("hi")
        advanceUntilIdle()

        val result = controller.searchResult.value!!
        assertThat(result.searchQuery).isEqualTo("hi")
        assertThat(result.currentIndex).isEqualTo(0)
        assertThat(result.isLoading).isFalse()
        assertThat(result.messages.map { it.id }).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(scrolledTo).containsExactly(1L)
    }

    @Test
    fun `repeated query is ignored`() = runTest {
        stubSearch(1, 2)
        val controller = controller()

        controller.search("hi")
        advanceUntilIdle()
        controller.search("hi")
        advanceUntilIdle()

        verifyBlocking(messageInteractor, times(1)) { searchMessages(any(), any(), any()) }
    }

    @Test
    fun `scroll steps forward through results`() = runTest {
        stubSearch(1, 2, 3)
        val controller = controller()
        controller.search("hi")
        advanceUntilIdle()

        preparingToScroll.set(false)
        controller.scrollToSearchMessage(isPrev = true)
        preparingToScroll.set(false)
        controller.scrollToSearchMessage(isPrev = true)

        assertThat(scrolledTo).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `scroll is ignored while another scroll is preparing`() = runTest {
        stubSearch(1, 2, 3)
        val controller = controller()
        controller.search("hi")
        advanceUntilIdle()

        // search left preparing untouched; force the guard on.
        preparingToScroll.set(true)
        controller.scrollToSearchMessage(isPrev = true)

        assertThat(scrolledTo).containsExactly(1L)
    }

    @Test
    fun `scroll past the edge does nothing`() = runTest {
        stubSearch(1, 2, 3)
        val controller = controller()
        controller.search("hi")
        advanceUntilIdle()

        preparingToScroll.set(false)
        // currentIndex is 0; isPrev=false steps to -1 -> out of range.
        controller.scrollToSearchMessage(isPrev = false)

        assertThat(scrolledTo).containsExactly(1L)
    }

    @Test
    fun `nearing the end with more pages loads and appends the next batch`() = runTest {
        stubSearch(1, 2, 3, 4, 5, hasNext = true)
        whenever { messageInteractor.loadNextSearchMessages() }
            .thenReturn(SceytPagingResponse.Success(listOf(msg(6), msg(7)), hasNext = false))
        val controller = controller(queryLimit = 10)
        controller.search("hi")
        advanceUntilIdle()

        preparingToScroll.set(false)
        controller.scrollToSearchMessage(isPrev = true)
        advanceUntilIdle()

        verifyBlocking(messageInteractor) { loadNextSearchMessages() }
        val result = controller.searchResult.value!!
        assertThat(result.messages.map { it.id }).containsExactly(1L, 2L, 3L, 4L, 5L, 7L, 6L).inOrder()
        assertThat(result.hasNext).isFalse()
    }
}
