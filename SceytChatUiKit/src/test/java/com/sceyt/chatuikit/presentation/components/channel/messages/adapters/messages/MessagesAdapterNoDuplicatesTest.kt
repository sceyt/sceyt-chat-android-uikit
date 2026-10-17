package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.presentation.common.collections.SyncArrayList
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/**
 * A message is identified by its tid, so the list must never hold two rows of the same message.
 *
 * Repro this locks: a message sent while offline is displayed as pending (id 0), and after
 * reconnect it comes back from the server with its id, either inside a pagination page or as a
 * sync result. It must update the displayed row, not add a second one.
 */
@RunWith(RobolectricTestRunner::class)
class MessagesAdapterNoDuplicatesTest {

    private fun adapter(initial: List<MessageListItem> = emptyList()): MessagesAdapter =
        MessagesAdapter(
            messages = SyncArrayList(initial),
            viewHolderFactory = mock(),
            style = mock(),
            scope = mock(),
            recyclerView = mock(),
        )

    private fun pendingItem(tid: Long) = MessageListItem.MessageItem(
        createMessage(createdAt = tid, id = 0, tid = tid)
            .copy(deliveryStatus = MessageDeliveryStatus.Pending)
    )

    private fun sentItem(tid: Long, id: Long) = MessageListItem.MessageItem(
        createMessage(createdAt = tid, id = id, tid = tid)
            .copy(deliveryStatus = MessageDeliveryStatus.Sent)
    )

    private fun MessagesAdapter.messageItems() =
        getData().filterIsInstance<MessageListItem.MessageItem>()

    @Test
    fun `addPrevPageMessagesList does not add a message which is already displayed`() {
        val adapter = adapter(listOf(pendingItem(10)))

        adapter.addPrevPageMessagesList(listOf(sentItem(1, id = 100), sentItem(10, id = 110)))

        val items = adapter.messageItems()
        assertThat(items.map { it.message.tid }).containsExactly(1L, 10L).inOrder()
    }

    @Test
    fun `addPrevPageMessagesList updates the already displayed message in place`() {
        val adapter = adapter(listOf(pendingItem(10)))

        adapter.addPrevPageMessagesList(listOf(sentItem(10, id = 110)))

        val item = adapter.messageItems().single()
        assertThat(item.message.tid).isEqualTo(10L)
        assertThat(item.message.id).isEqualTo(110L)
        assertThat(item.message.deliveryStatus).isEqualTo(MessageDeliveryStatus.Sent)
    }

    @Test
    fun `addNextPageMessagesList does not add a message which is already displayed`() {
        val adapter = adapter(listOf(pendingItem(10)))

        adapter.addNextPageMessagesList(listOf(sentItem(10, id = 110), sentItem(11, id = 111)))

        assertThat(adapter.messageItems().map { it.message.tid }).containsExactly(10L, 11L).inOrder()
    }

    @Test
    fun `addNewMessages does not add a message which is already displayed with another state`() {
        val adapter = adapter(listOf(pendingItem(10)))

        adapter.addNewMessages(listOf(sentItem(10, id = 110)))

        val item = adapter.messageItems().single()
        assertThat(item.message.id).isEqualTo(110L)
        assertThat(item.message.deliveryStatus).isEqualTo(MessageDeliveryStatus.Sent)
    }

    @Test
    fun `date separator of a skipped message is skipped too`() {
        val adapter = adapter(listOf(pendingItem(10)))

        adapter.addPrevPageMessagesList(
            listOf(
                MessageListItem.DateSeparatorItem(createdAt = 10, messageTid = 10, messageId = 110),
                sentItem(10, id = 110)
            )
        )

        assertThat(adapter.getData().filterIsInstance<MessageListItem.DateSeparatorItem>()).isEmpty()
        assertThat(adapter.messageItems()).hasSize(1)
    }
}