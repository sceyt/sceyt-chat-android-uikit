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

    private fun sentItemAt(tid: Long, id: Long, createdAt: Long) =
        sentItem(tid, id).let { item ->
            item.copy(message = item.message.copy(createdAt = createdAt))
        }

    private fun dateItem(item: MessageListItem.MessageItem) =
        MessageListItem.DateSeparatorItem(
            createdAt = item.message.createdAt,
            messageTid = item.message.tid,
            messageId = item.message.id,
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
    fun `server echo updates fields while preserving local ui state and ordering time`() {
        val pending = pendingItem(10).let { item ->
            item.copy(
                message = item.message.copy(
                    body = "local body",
                    createdAt = 1_000,
                    isSelected = true,
                    isBodyExpanded = true,
                )
            )
        }
        val server = sentItem(10, id = 110).let { item ->
            item.copy(message = item.message.copy(body = "server body", createdAt = 2_000))
        }
        val adapter = adapter(listOf(pending))

        adapter.addNewMessages(listOf(server))

        val message = adapter.messageItems().single().message
        assertThat(message.id).isEqualTo(110L)
        assertThat(message.body).isEqualTo("server body")
        assertThat(message.deliveryStatus).isEqualTo(MessageDeliveryStatus.Sent)
        assertThat(message.createdAt).isEqualTo(1_000L)
        assertThat(message.isSelected).isTrue()
        assertThat(message.isBodyExpanded).isTrue()
    }

    @Test
    fun `same day prepend keeps one date separator at the page boundary`() {
        val currentMessage = sentItemAt(tid = 20, id = 120, createdAt = 2_000)
        val adapter = adapter(listOf(dateItem(currentMessage), currentMessage))
        val previousMessage = sentItemAt(tid = 10, id = 110, createdAt = 1_000)

        adapter.addPrevPageMessagesList(
            listOf(dateItem(previousMessage), previousMessage)
        )

        assertThat(adapter.getData())
            .containsExactly(dateItem(previousMessage), previousMessage, currentMessage)
            .inOrder()
    }

    @Test
    fun `cross day prepend keeps both date separators at the page boundary`() {
        val currentMessage = sentItemAt(tid = 20, id = 120, createdAt = 86_401_000)
        val adapter = adapter(listOf(dateItem(currentMessage), currentMessage))
        val previousMessage = sentItemAt(tid = 10, id = 110, createdAt = 1_000)

        adapter.addPrevPageMessagesList(
            listOf(dateItem(previousMessage), previousMessage)
        )

        assertThat(adapter.getData())
            .containsExactly(
                dateItem(previousMessage),
                previousMessage,
                dateItem(currentMessage),
                currentMessage,
            )
            .inOrder()
    }

    @Test
    fun `empty pages remove only their matching edge loader`() {
        val message = sentItem(tid = 10, id = 110)
        val previousAdapter = adapter(
            listOf(MessageListItem.LoadingPrevItem, message, MessageListItem.LoadingNextItem)
        )
        val nextAdapter = adapter(
            listOf(MessageListItem.LoadingPrevItem, message, MessageListItem.LoadingNextItem)
        )

        previousAdapter.addPrevPageMessagesList(emptyList())
        nextAdapter.addNextPageMessagesList(emptyList())

        assertThat(previousAdapter.getData())
            .containsExactly(message, MessageListItem.LoadingNextItem)
            .inOrder()
        assertThat(nextAdapter.getData())
            .containsExactly(MessageListItem.LoadingPrevItem, message)
            .inOrder()
    }

    @Test
    fun `duplicate only pages remove matching loader and retain the other edge`() {
        val message = sentItem(tid = 10, id = 110)
        val previousAdapter = adapter(
            listOf(MessageListItem.LoadingPrevItem, message, MessageListItem.LoadingNextItem)
        )
        val nextAdapter = adapter(
            listOf(MessageListItem.LoadingPrevItem, message, MessageListItem.LoadingNextItem)
        )

        previousAdapter.addPrevPageMessagesList(listOf(sentItem(tid = 10, id = 110)))
        nextAdapter.addNextPageMessagesList(listOf(sentItem(tid = 10, id = 110)))

        assertThat(previousAdapter.getData())
            .containsExactly(message, MessageListItem.LoadingNextItem)
            .inOrder()
        assertThat(nextAdapter.getData())
            .containsExactly(MessageListItem.LoadingPrevItem, message)
            .inOrder()
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
