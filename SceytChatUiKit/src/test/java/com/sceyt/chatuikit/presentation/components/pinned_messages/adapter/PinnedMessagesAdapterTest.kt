package com.sceyt.chatuikit.presentation.components.pinned_messages.adapter

import androidx.lifecycle.LifecycleCoroutineScope
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.presentation.common.collections.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessagesAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/**
 * The chat screen keeps updating its [MessagesAdapter] while the pinned messages screen is open.
 * Neither adapter's update may cancel the other's.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PinnedMessagesAdapterTest {

    private fun item(tid: Long): MessageListItem.MessageItem =
        MessageListItem.MessageItem(createMessage(createdAt = tid, tid = tid))

    private fun MessagesAdapter.tids() =
        getData().map { (it as MessageListItem.MessageItem).message.tid }

    private class Adapters(val chat: MessagesAdapter, val pinned: PinnedMessagesAdapter)

    private fun TestScope.adapters(): Adapters {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = mock<LifecycleCoroutineScope> {
            on { coroutineContext } doReturn dispatcher
        }
        val chat = MessagesAdapter(
            messages = SyncArrayList(),
            viewHolderFactory = mock(),
            style = mock(),
            scope = scope,
            // Mock RV has a null adapter, so dispatchUpdatesToSafetySuspend resumes without a looper.
            recyclerView = mock(),
            backgroundDispatcher = dispatcher,
            mainDispatcher = dispatcher,
        )
        val pinned = PinnedMessagesAdapter(
            viewHolderFactory = mock(),
            messagesListStyle = mock(),
            scope = scope,
            recyclerView = mock(),
            style = mock(),
            onNavigateClick = {},
            backgroundDispatcher = dispatcher,
        )
        return Adapters(chat, pinned)
    }

    @Test
    fun `chat update does not cancel pending pinned update`() = runTest {
        val (chat, pinned) = adapters().let { it.chat to it.pinned }

        pinned.submitList(listOf(item(1), item(2)))
        chat.notifyUpdate(listOf(item(3)))
        advanceUntilIdle()

        assertThat(pinned.tids()).containsExactly(1L, 2L).inOrder()
        assertThat(chat.tids()).containsExactly(3L)
    }

    @Test
    fun `pinned update does not cancel pending chat update`() = runTest {
        val (chat, pinned) = adapters().let { it.chat to it.pinned }

        chat.notifyUpdate(listOf(item(3)))
        pinned.submitList(listOf(item(1), item(2)))
        advanceUntilIdle()

        assertThat(chat.tids()).containsExactly(3L)
        assertThat(pinned.tids()).containsExactly(1L, 2L).inOrder()
    }
}
