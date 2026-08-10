package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class MessageListStateTest {

    @Test
    fun `same tid content update publishes a new state revision`() = runTest {
        val state = MessageListState(enableDateSeparator = false)
        val pending = item(tid = 10, id = 0, status = MessageDeliveryStatus.Pending)
        val sent = item(tid = 10, id = 100, status = MessageDeliveryStatus.Sent)
        state.replace(listOf(pending))
        val nextState = async { state.state.drop(1).first() }
        runCurrent()

        state.appendRealtime(listOf(sent))

        val emitted = nextState.await()
        assertThat(emitted.revision).isEqualTo(2)
        assertThat(emitted.items.filterIsInstance<MessageItem>().single().message.id).isEqualTo(100)
    }

    @Test
    fun `missing update does not publish a false revision`() {
        val state = MessageListState(enableDateSeparator = false)
        state.replace(listOf(item(tid = 1)))
        val before = state.state.value

        state.updateByTid(1) { it }
        state.updateByTid(999) { it.copy(message = it.message.copy(body = "unused")) }

        assertThat(state.state.value).isSameInstanceAs(before)
    }

    @Test
    fun `late reader receives the latest canonical snapshot`() {
        val state = MessageListState(enableDateSeparator = false)
        state.replace(listOf(item(tid = 1)))
        state.appendRealtime(listOf(item(tid = 2)))

        val latest = state.state.value

        assertThat(latest.items.filterIsInstance<MessageItem>().map { it.message.tid })
            .containsExactly(1L, 2L)
            .inOrder()
        assertThat(latest.revision).isEqualTo(2)
    }

    @Test
    fun `concurrent appends do not lose writes or duplicate tids`() = runBlocking {
        val state = MessageListState(enableDateSeparator = false)
        val count = 200

        (1..count).map { id ->
            launch(Dispatchers.Default) {
                state.appendRealtime(listOf(item(tid = id.toLong())))
            }
        }.joinAll()

        val snapshot = state.state.value
        val tids = snapshot.items.filterIsInstance<MessageItem>().map { it.message.tid }
        assertThat(tids).containsExactlyElementsIn((1L..count.toLong()).toList())
        assertThat(tids.toSet()).hasSize(count)
        assertThat(snapshot.revision).isEqualTo(count)
    }

    @Test
    fun `concurrent updates run each callback once and lose no changes`() = runBlocking {
        val state = MessageListState(enableDateSeparator = false)
        val callbackCount = AtomicInteger()
        val updateCount = 200
        state.replace(listOf(item(tid = 1)))

        (1..updateCount).map {
            launch(Dispatchers.Default) {
                state.updateByTid(1) { item ->
                    callbackCount.incrementAndGet()
                    item.copy(message = item.message.copy(replyCount = item.message.replyCount + 1))
                }
            }
        }.joinAll()

        val message = state.state.value.items.filterIsInstance<MessageItem>().single().message
        assertThat(callbackCount.get()).isEqualTo(updateCount)
        assertThat(message.replyCount).isEqualTo(updateCount)
        assertThat(state.state.value.revision).isEqualTo(updateCount + 1L)
    }

    @Test
    fun `same tid echoes cannot regress server identity or status in any arrival order`() {
        val arrivalOrders = listOf(
            listOf(MessageDeliveryStatus.Pending, MessageDeliveryStatus.Sent, MessageDeliveryStatus.Displayed),
            listOf(MessageDeliveryStatus.Pending, MessageDeliveryStatus.Displayed, MessageDeliveryStatus.Sent),
            listOf(MessageDeliveryStatus.Sent, MessageDeliveryStatus.Pending, MessageDeliveryStatus.Displayed),
            listOf(MessageDeliveryStatus.Sent, MessageDeliveryStatus.Displayed, MessageDeliveryStatus.Pending),
            listOf(MessageDeliveryStatus.Displayed, MessageDeliveryStatus.Pending, MessageDeliveryStatus.Sent),
            listOf(MessageDeliveryStatus.Displayed, MessageDeliveryStatus.Sent, MessageDeliveryStatus.Pending),
        )

        arrivalOrders.forEach { statuses ->
            val state = MessageListState(enableDateSeparator = false)
            val pending = item(tid = 1, id = 0, status = MessageDeliveryStatus.Pending)
            state.replace(listOf(pending))

            statuses.forEach { status ->
                val id = if (status == MessageDeliveryStatus.Pending) 0L else 100L
                state.appendRealtime(listOf(item(tid = 1, id = id, status = status)))
            }

            val message = state.state.value.items.filterIsInstance<MessageItem>().single().message
            assertThat(message.id).isEqualTo(100)
            assertThat(message.deliveryStatus).isEqualTo(MessageDeliveryStatus.Displayed)
        }
    }

    @Test
    fun `state does not retain the mutable input list`() {
        val state = MessageListState(enableDateSeparator = false)
        val first = item(tid = 1)
        val input = mutableListOf(first)

        state.replace(input)
        input.add(item(tid = 2))

        assertThat(state.state.value.items).containsExactly(first)
    }

    private fun item(
        tid: Long,
        id: Long = tid,
        status: MessageDeliveryStatus = MessageDeliveryStatus.Displayed,
    ) = MessageItem(
        createMessage(createdAt = tid, id = id, tid = tid).copy(deliveryStatus = status)
    )
}
