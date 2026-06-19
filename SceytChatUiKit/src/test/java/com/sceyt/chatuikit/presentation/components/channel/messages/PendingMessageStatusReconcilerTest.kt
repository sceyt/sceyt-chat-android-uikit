package com.sceyt.chatuikit.presentation.components.channel.messages

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import org.junit.Test

/**
 * Reproduces the "forwarded message stuck on Pending" bug and verifies the fix:
 * a status update that arrives while the message is missing from the list must be re-applied
 * once the list commits the message.
 */
class PendingMessageStatusReconcilerTest {

    /**
     * Stand-in for the visible message list. apply() mirrors MessagesListView.updateMessage:
     * it updates and returns true only if the message (by tid) is present, otherwise false.
     */
    private class FakeMessageList {
        private val statuses = HashMap<Long, MessageDeliveryStatus>()

        fun render(tid: Long, status: MessageDeliveryStatus) {
            statuses[tid] = status
        }

        fun statusOf(tid: Long): MessageDeliveryStatus? = statuses[tid]

        fun apply(message: SceytMessage): Boolean {
            if (!statuses.containsKey(message.tid)) return false
            statuses[message.tid] = message.deliveryStatus
            return true
        }
    }

    private fun message(tid: Long, status: MessageDeliveryStatus): SceytMessage =
        createMessage(createdAt = tid, tid = tid).copy(deliveryStatus = status)

    @Test
    fun `update for a missing message is parked, not lost`() {
        val reconciler = PendingMessageStatusReconciler()
        val list = FakeMessageList() // message tid=1 not in the list yet

        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Sent), list::apply)

        assertThat(reconciler.parkedCount).isEqualTo(1)
        assertThat(list.statusOf(1)).isNull()
    }

    @Test
    fun `parked update is applied once the list commits the message`() {
        val reconciler = PendingMessageStatusReconciler()
        val list = FakeMessageList()

        // Sent arrives while the message is still absent (mid rebuild) -> parked.
        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Sent), list::apply)
        // The message is then rendered as Pending (e.g. by onOutgoingMessage).
        list.render(1, MessageDeliveryStatus.Pending)

        // List commit -> reconcile re-applies the parked Sent.
        reconciler.reconcile(list::apply)

        assertThat(list.statusOf(1)).isEqualTo(MessageDeliveryStatus.Sent)
        assertThat(reconciler.parkedCount).isEqualTo(0)
    }

    @Test
    fun `without reconcile the message stays stuck on Pending`() {
        // Regression guard: this is exactly the original bug if reconcile is never called.
        val reconciler = PendingMessageStatusReconciler()
        val list = FakeMessageList()

        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Sent), list::apply)
        list.render(1, MessageDeliveryStatus.Pending)

        assertThat(list.statusOf(1)).isEqualTo(MessageDeliveryStatus.Pending)
        assertThat(reconciler.parkedCount).isEqualTo(1)
    }

    @Test
    fun `update for a present message is applied immediately and not parked`() {
        val reconciler = PendingMessageStatusReconciler()
        val list = FakeMessageList()
        list.render(1, MessageDeliveryStatus.Pending)

        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Sent), list::apply)

        assertThat(list.statusOf(1)).isEqualTo(MessageDeliveryStatus.Sent)
        assertThat(reconciler.parkedCount).isEqualTo(0)
    }

    @Test
    fun `reconcile keeps still-missing updates parked and drops applied ones`() {
        val reconciler = PendingMessageStatusReconciler()
        val list = FakeMessageList()

        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Sent), list::apply)
        reconciler.onStatusUpdate(message(2, MessageDeliveryStatus.Sent), list::apply)
        assertThat(reconciler.parkedCount).isEqualTo(2)

        // Only tid=1 becomes present.
        list.render(1, MessageDeliveryStatus.Pending)
        reconciler.reconcile(list::apply)

        assertThat(list.statusOf(1)).isEqualTo(MessageDeliveryStatus.Sent)
        assertThat(reconciler.parkedCount).isEqualTo(1) // tid=2 still parked
    }

    @Test
    fun `latest parked update wins for the same tid`() {
        val reconciler = PendingMessageStatusReconciler()
        val list = FakeMessageList()

        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Sent), list::apply)
        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Received), list::apply)

        list.render(1, MessageDeliveryStatus.Pending)
        reconciler.reconcile(list::apply)

        assertThat(list.statusOf(1)).isEqualTo(MessageDeliveryStatus.Received)
    }

    @Test
    fun `take removes and returns the parked update`() {
        val reconciler = PendingMessageStatusReconciler()
        val list = FakeMessageList()
        reconciler.onStatusUpdate(message(1, MessageDeliveryStatus.Sent), list::apply)

        val taken = reconciler.take(1)

        assertThat(taken?.deliveryStatus).isEqualTo(MessageDeliveryStatus.Sent)
        assertThat(reconciler.parkedCount).isEqualTo(0)
        assertThat(reconciler.take(1)).isNull()
    }
}
