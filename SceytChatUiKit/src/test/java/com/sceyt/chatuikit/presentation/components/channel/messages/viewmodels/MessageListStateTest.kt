package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
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

    @Test
    fun `mutation results distinguish missing unchanged and committed updates`() {
        val state = MessageListState(enableDateSeparator = false)

        val inserted = state.replace(listOf(item(tid = 1)))
        val unchanged = state.updateByTid(1) { it }
        val missing = state.updateByTid(999) {
            it.copy(message = it.message.copy(body = "never called"))
        }
        val changed = state.updateByTid(1) {
            it.copy(message = it.message.copy(body = "edited"))
        }

        assertThat(inserted).isEqualTo(MessageListState.MutationResult(revision = 1, changed = true))
        assertThat(unchanged).isEqualTo(
            MessageListState.ItemUpdateResult(revision = 1, found = true, changed = false)
        )
        assertThat(missing).isEqualTo(
            MessageListState.ItemUpdateResult(revision = 1, found = false, changed = false)
        )
        assertThat(changed).isEqualTo(
            MessageListState.ItemUpdateResult(revision = 2, found = true, changed = true)
        )
    }

    @Test
    fun `loader removals report the revision that contains the change`() {
        val state = MessageListState(enableDateSeparator = false)
        state.replace(
            listOf(
                MessageListItem.LoadingPrevItem,
                item(tid = 1),
                MessageListItem.LoadingNextItem,
            )
        )

        val removed = state.hideLoadingPrev()
        val alreadyRemoved = state.hideLoadingPrev()

        assertThat(removed).isEqualTo(MessageListState.MutationResult(revision = 2, changed = true))
        assertThat(alreadyRemoved)
            .isEqualTo(MessageListState.MutationResult(revision = 2, changed = false))
        assertThat(state.state.value.items)
            .containsExactly(item(tid = 1), MessageListItem.LoadingNextItem)
            .inOrder()
    }

    @Test
    fun `next loader removal preserves previous edge and is idempotent`() {
        val state = MessageListState(enableDateSeparator = false)
        state.replace(
            listOf(
                MessageListItem.LoadingPrevItem,
                item(tid = 1),
                MessageListItem.LoadingNextItem,
            )
        )

        val removed = state.hideLoadingNext()
        val alreadyRemoved = state.hideLoadingNext()

        assertThat(removed).isEqualTo(MessageListState.MutationResult(revision = 2, changed = true))
        assertThat(alreadyRemoved)
            .isEqualTo(MessageListState.MutationResult(revision = 2, changed = false))
        assertThat(state.state.value.items)
            .containsExactly(MessageListItem.LoadingPrevItem, item(tid = 1))
            .inOrder()
    }

    @Test
    fun `unread removal preserves loaders repairs avatar and then becomes a no-op`() {
        val user = SceytUser("same-user")
        val first = item(tid = 1).let { item ->
            item.copy(message = item.message.copy(incoming = true, isGroup = true, user = user))
        }
        val second = item(tid = 2).let { item ->
            item.copy(message = item.message.copy(incoming = true, isGroup = true, user = user))
        }
        val unread = MessageListItem.UnreadMessagesSeparatorItem(
            createdAt = second.message.createdAt,
            msgId = 10,
        )
        val state = MessageListState(enableDateSeparator = false)
        state.replace(
            listOf(
                MessageListItem.LoadingPrevItem,
                first,
                unread,
                second,
                MessageListItem.LoadingNextItem,
            )
        )

        val removed = state.removeUnreadSeparator()
        val repeated = state.removeUnreadSeparator()

        assertThat(removed).isEqualTo(MessageListState.MutationResult(revision = 2, changed = true))
        assertThat(repeated).isEqualTo(MessageListState.MutationResult(revision = 2, changed = false))
        assertThat(state.state.value.items.first()).isEqualTo(MessageListItem.LoadingPrevItem)
        assertThat(state.state.value.items.last()).isEqualTo(MessageListItem.LoadingNextItem)
        assertThat(state.state.value.items.filterIsInstance<MessageItem>()
            .map { it.message.shouldShowAvatarAndName })
            .containsExactly(true, false)
            .inOrder()
    }

    @Test
    fun `batch reconciliation updates main and reply in one revision`() {
        val state = MessageListState(enableDateSeparator = false)
        val parent = item(tid = 1, id = 100)
        val reply = item(tid = 2, id = 200).let { item ->
            item.copy(message = item.message.copy(parentMessage = parent.message))
        }
        state.replace(listOf(parent, reply))
        val update = parent.copy(message = parent.message.copy(body = "edited"))

        val result = state.reconcileMessages(
            rowOnlyUpdates = emptyList(),
            rowAndReplyUpdates = listOf(update),
        )

        assertThat(result).isEqualTo(MessageListState.MutationResult(revision = 2, changed = true))
        assertThat(state.state.value.revision).isEqualTo(2)
        val messages = state.state.value.items.filterIsInstance<MessageItem>()
        assertThat(messages[0].message.body).isEqualTo("edited")
        assertThat(messages[1].message.parentMessage?.body).isEqualTo("edited")
    }

    @Test
    fun `mixed batch keeps row-only updates out of previews and commits once`() {
        val state = MessageListState(enableDateSeparator = false)
        val statusParent = item(tid = 1, id = 100, status = MessageDeliveryStatus.Sent)
        val editedParent = item(tid = 2, id = 200, status = MessageDeliveryStatus.Sent)
        val statusReply = item(tid = 3, id = 300).let { item ->
            item.copy(message = item.message.copy(parentMessage = statusParent.message))
        }
        val firstEditedReply = item(tid = 4, id = 400).let { item ->
            item.copy(message = item.message.copy(parentMessage = editedParent.message))
        }
        val secondEditedReply = item(tid = 5, id = 500).let { item ->
            item.copy(message = item.message.copy(parentMessage = editedParent.message))
        }
        state.replace(
            listOf(statusParent, editedParent, statusReply, firstEditedReply, secondEditedReply)
        )
        val statusUpdate = statusParent.copy(
            message = statusParent.message.copy(
                body = "status-only",
                deliveryStatus = MessageDeliveryStatus.Received,
            )
        )
        val editUpdate = editedParent.copy(
            message = editedParent.message.copy(body = "edited")
        )

        val result = state.reconcileMessages(
            rowOnlyUpdates = listOf(statusUpdate),
            rowAndReplyUpdates = listOf(editUpdate),
        )

        assertThat(result).isEqualTo(MessageListState.MutationResult(revision = 2, changed = true))
        val messages = state.state.value.items.filterIsInstance<MessageItem>()
        assertThat(messages[0].message.body).isEqualTo("status-only")
        assertThat(messages[2].message.parentMessage?.body).isEmpty()
        assertThat(messages[3].message.parentMessage?.body).isEqualTo("edited")
        assertThat(messages[4].message.parentMessage?.body).isEqualTo("edited")
    }

    @Test
    fun `history trim removes previous edge and failed rows but keeps pending at cutoff`() {
        val state = MessageListState(enableDateSeparator = false)
        val pending = item(tid = 2, id = 0, status = MessageDeliveryStatus.Pending)
        val failed = item(tid = 3, id = 0, status = MessageDeliveryStatus.Failed)
        val newer = item(tid = 4, status = MessageDeliveryStatus.Sent)
        state.replace(
            listOf(
                MessageListItem.LoadingPrevItem,
                pending,
                failed,
                newer,
                MessageListItem.LoadingNextItem,
            )
        )

        val trimmed = state.deleteAtOrBeforePreservingPending(createdAt = 3)
        val repeated = state.deleteAtOrBeforePreservingPending(createdAt = 3)

        assertThat(trimmed).isEqualTo(MessageListState.MutationResult(revision = 2, changed = true))
        assertThat(repeated).isEqualTo(MessageListState.MutationResult(revision = 2, changed = false))
        assertThat(state.state.value.items)
            .containsExactly(pending, newer, MessageListItem.LoadingNextItem)
            .inOrder()
    }

    @Test
    fun `clear selection changes all rows in one revision and then becomes a no-op`() {
        val state = MessageListState(enableDateSeparator = false)
        state.replace(
            listOf(
                item(tid = 1).let { it.copy(message = it.message.copy(isSelected = true)) },
                item(tid = 2).let { it.copy(message = it.message.copy(isSelected = true)) },
            )
        )

        val cleared = state.clearSelection()
        val repeated = state.clearSelection()

        assertThat(cleared).isEqualTo(MessageListState.MutationResult(revision = 2, changed = true))
        assertThat(repeated).isEqualTo(MessageListState.MutationResult(revision = 2, changed = false))
        assertThat(
            state.state.value.items.filterIsInstance<MessageItem>().map { it.message.isSelected }
        ).containsExactly(false, false)
    }

    private fun item(
        tid: Long,
        id: Long = tid,
        status: MessageDeliveryStatus = MessageDeliveryStatus.Displayed,
    ) = MessageItem(
        createMessage(createdAt = tid, id = id, tid = tid).copy(deliveryStatus = status)
    )
}
