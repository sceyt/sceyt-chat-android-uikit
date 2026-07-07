package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import org.junit.Test

class MessagesDiffUtilTest {

    private fun messageItem(
        tid: Long,
        id: Long = tid,
        body: String = "",
    ) = MessageListItem.MessageItem(
        createMessage(createdAt = id, id = id, tid = tid).copy(body = body)
    )

    @Test
    fun `message identity uses tid and exposes payload`() {
        val oldItem = messageItem(tid = 10, id = 1, body = "old")
        val newItem = messageItem(tid = 10, id = 2, body = "new")
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areItemsTheSame(0, 0)).isTrue()
        assertThat(diff.areContentsTheSame(0, 0)).isFalse()

        val payload = diff.getChangePayload(0, 0) as MessageDiff
        assertThat(payload.bodyChanged).isTrue()
    }

    @Test
    fun `different message tids are different items`() {
        val diff = MessagesDiffUtil(
            oldList = listOf(messageItem(tid = 10)),
            newList = listOf(messageItem(tid = 11))
        )

        assertThat(diff.areItemsTheSame(0, 0)).isFalse()
    }

    @Test
    fun `date separator identity uses owning message tid`() {
        val oldItem = MessageListItem.DateSeparatorItem(
            createdAt = 1,
            messageTid = 10,
            messageId = 100
        )
        val newItem = oldItem.copy(createdAt = 2)
        val diff = MessagesDiffUtil(listOf(oldItem), listOf(newItem))

        assertThat(diff.areItemsTheSame(0, 0)).isTrue()
        assertThat(diff.areContentsTheSame(0, 0)).isFalse()
    }

    @Test
    fun `loading items keep stable identity`() {
        val prevDiff = MessagesDiffUtil(
            oldList = listOf(MessageListItem.LoadingPrevItem),
            newList = listOf(MessageListItem.LoadingPrevItem)
        )
        val nextDiff = MessagesDiffUtil(
            oldList = listOf(MessageListItem.LoadingNextItem),
            newList = listOf(MessageListItem.LoadingNextItem)
        )

        assertThat(prevDiff.areItemsTheSame(0, 0)).isTrue()
        assertThat(prevDiff.areContentsTheSame(0, 0)).isTrue()
        assertThat(nextDiff.areItemsTheSame(0, 0)).isTrue()
        assertThat(nextDiff.areContentsTheSame(0, 0)).isTrue()
    }
}
