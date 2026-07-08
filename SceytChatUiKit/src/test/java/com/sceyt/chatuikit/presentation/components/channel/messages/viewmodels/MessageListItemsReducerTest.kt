package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.DateSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import org.junit.Test

class MessageListItemsReducerTest {
    private val reducer = MessageListItemsReducer()

    @Test
    fun `normalize collapses same day date separators`() {
        val firstDate = dateItem(createdAt = 1_000, tid = 1)
        val secondDate = dateItem(createdAt = 2_000, tid = 2)
        val items = listOf(
            firstDate,
            messageItem(createdAt = 1_000, id = 1),
            secondDate,
            messageItem(createdAt = 2_000, id = 2)
        )

        val result = reducer.normalize(items, enableDateSeparator = true)

        assertThat(result.filterIsInstance<DateSeparatorItem>()).containsExactly(firstDate)
        assertThat(result.filterIsInstance<MessageItem>().map { it.message.id })
            .containsExactly(1L, 2L)
            .inOrder()
    }

    @Test
    fun `normalize keeps date separators for different days`() {
        val firstDate = dateItem(createdAt = 1_000, tid = 1)
        val secondDate = dateItem(createdAt = 86_401_000, tid = 2)

        val result = reducer.normalize(
            items = listOf(firstDate, messageItem(1_000, 1), secondDate, messageItem(86_401_000, 2)),
            enableDateSeparator = true
        )

        assertThat(result.filterIsInstance<DateSeparatorItem>()).containsExactly(firstDate, secondDate).inOrder()
    }

    @Test
    fun `merge prev page removes loading item when incoming is empty`() {
        val message = messageItem(createdAt = 1_000, id = 1)

        val result = reducer.mergePrevPage(
            current = listOf(MessageListItem.LoadingPrevItem, message),
            newItems = emptyList(),
            enableDateSeparator = true
        )

        assertThat(result).containsExactly(message)
    }

    @Test
    fun `merge prev page removes old boundary date separator for same day`() {
        val newDate = dateItem(createdAt = 1_000, tid = 1)
        val newMessage = messageItem(createdAt = 1_000, id = 1)
        val oldDate = dateItem(createdAt = 2_000, tid = 2)
        val oldMessage = messageItem(createdAt = 2_000, id = 2)

        val result = reducer.mergePrevPage(
            current = listOf(MessageListItem.LoadingPrevItem, oldDate, oldMessage),
            newItems = listOf(newDate, newMessage),
            enableDateSeparator = true
        )

        assertThat(result).containsExactly(newDate, newMessage, oldMessage).inOrder()
    }

    @Test
    fun `merge prev page keeps boundary date separator for different days`() {
        val newDate = dateItem(createdAt = 1_000, tid = 1)
        val newMessage = messageItem(createdAt = 1_000, id = 1)
        val oldDate = dateItem(createdAt = 86_401_000, tid = 2)
        val oldMessage = messageItem(createdAt = 86_401_000, id = 2)

        val result = reducer.mergePrevPage(
            current = listOf(MessageListItem.LoadingPrevItem, oldDate, oldMessage),
            newItems = listOf(newDate, newMessage),
            enableDateSeparator = true
        )

        assertThat(result).containsExactly(newDate, newMessage, oldDate, oldMessage).inOrder()
    }

    @Test
    fun `append next page removes loading item and returns inserted items`() {
        val currentMessage = messageItem(createdAt = 1_000, id = 1)
        val newMessage = messageItem(createdAt = 2_000, id = 2)

        val result = reducer.appendNextPage(
            current = listOf(currentMessage, MessageListItem.LoadingNextItem),
            newItems = listOf(newMessage)
        )

        assertThat(result.changed).isTrue()
        assertThat(result.resultItems).containsExactly(currentMessage, newMessage).inOrder()
        assertThat(result.insertedItems).containsExactly(newMessage)
    }

    @Test
    fun `append next page reports loading removal when incoming is empty`() {
        val currentMessage = messageItem(createdAt = 1_000, id = 1)

        val result = reducer.appendNextPage(
            current = listOf(currentMessage, MessageListItem.LoadingNextItem),
            newItems = emptyList()
        )

        assertThat(result.changed).isTrue()
        assertThat(result.resultItems).containsExactly(currentMessage)
        assertThat(result.insertedItems).isEmpty()
    }

    @Test
    fun `append realtime ignores existing and duplicate incoming items`() {
        val existing = messageItem(createdAt = 1_000, id = 1)
        val newMessage = messageItem(createdAt = 2_000, id = 2)

        val result = reducer.appendRealtime(
            current = listOf(existing),
            newItems = listOf(existing, newMessage, newMessage)
        )

        assertThat(result.changed).isTrue()
        assertThat(result.resultItems).containsExactly(existing, newMessage).inOrder()
        assertThat(result.insertedItems).containsExactly(newMessage)
    }

    @Test
    fun `append realtime does not remove loading item when every incoming item already exists`() {
        val existing = messageItem(createdAt = 1_000, id = 1)

        val result = reducer.appendRealtime(
            current = listOf(existing, MessageListItem.LoadingNextItem),
            newItems = listOf(existing)
        )

        assertThat(result.changed).isFalse()
        assertThat(result.resultItems).containsExactly(existing, MessageListItem.LoadingNextItem).inOrder()
        assertThat(result.insertedItems).isEmpty()
    }

    private fun messageItem(createdAt: Long, id: Long): MessageItem {
        return MessageItem(createMessage(createdAt = createdAt, id = id, tid = id))
    }

    private fun dateItem(createdAt: Long, tid: Long): DateSeparatorItem {
        return DateSeparatorItem(createdAt = createdAt, messageTid = tid, messageId = tid)
    }
}
