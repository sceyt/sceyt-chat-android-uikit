package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import androidx.lifecycle.LifecycleCoroutineScope
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.presentation.common.collections.SyncArrayList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/**
 * Locks the contract the stuck-"Pending" fix relies on: every committed list mutation invokes
 * [MessagesAdapter.onListCommittedListener], so parked status updates get retried once the list
 * settles (see MessagesListViewBinding.flushNotFoundStatusUpdates).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MessagesAdapterListCommittedTest {

    private fun adapter(initial: List<MessageListItem> = emptyList()): MessagesAdapter =
        MessagesAdapter(
            messages = SyncArrayList(initial),
            viewHolderFactory = mock(),
            style = mock(),
            scope = mock(),
            recyclerView = mock(),
        )

    private fun item(tid: Long): MessageListItem.MessageItem =
        MessageListItem.MessageItem(createMessage(createdAt = tid, tid = tid))

    private fun MessagesAdapter.countCommits(): IntArray {
        val count = intArrayOf(0)
        onListCommittedListener = { count[0]++ }
        return count
    }

    @Test
    fun `addNewMessages invokes listener`() {
        val adapter = adapter()
        val count = adapter.countCommits()

        adapter.addNewMessages(listOf(item(1), item(2)))

        assertThat(count[0]).isEqualTo(1)
    }

    @Test
    fun `addNextPageMessagesList invokes listener`() {
        val adapter = adapter(listOf(item(1)))
        val count = adapter.countCommits()

        adapter.addNextPageMessagesList(listOf(item(2)))

        assertThat(count[0]).isEqualTo(1)
    }

    @Test
    fun `addPrevPageMessagesList invokes listener`() {
        val adapter = adapter(listOf(item(2)))
        val count = adapter.countCommits()

        adapter.addPrevPageMessagesList(listOf(item(1)))

        assertThat(count[0]).isEqualTo(1)
    }

    @Test
    fun `forceUpdate invokes listener`() {
        val adapter = adapter(listOf(item(1)))
        val count = adapter.countCommits()

        adapter.forceUpdate(listOf(item(1), item(2)))

        assertThat(count[0]).isEqualTo(1)
    }

    @Test
    fun `addNewMessages with no new items does not invoke listener`() {
        val adapter = adapter(listOf(item(1)))
        val count = adapter.countCommits()

        // Same item already present -> filtered out -> nothing committed.
        adapter.addNewMessages(listOf(item(1)))

        assertThat(count[0]).isEqualTo(0)
    }

    @Test
    fun `notifyUpdate invokes listener after the diff is committed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = mock<LifecycleCoroutineScope> {
            on { coroutineContext } doReturn dispatcher
        }
        val adapter = MessagesAdapter(
            messages = SyncArrayList(listOf(item(1))),
            viewHolderFactory = mock(),
            style = mock(),
            scope = scope,
            // Mock RV has a null adapter, so dispatchUpdatesToSafetySuspend resumes without a looper.
            recyclerView = mock(),
            backgroundDispatcher = dispatcher,
            mainDispatcher = dispatcher,
        )
        val count = adapter.countCommits()

        adapter.notifyUpdate(listOf(item(1), item(2)))
        advanceUntilIdle()

        assertThat(count[0]).isEqualTo(1)
    }
}
