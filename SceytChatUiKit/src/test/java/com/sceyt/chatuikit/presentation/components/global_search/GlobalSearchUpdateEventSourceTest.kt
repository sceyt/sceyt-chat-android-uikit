package com.sceyt.chatuikit.presentation.components.global_search

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for the pure list-transformation extension functions in GlobalSearchUpdateSource.kt:
 * [applyChannelMessageUpdateEvent] and [applyAttachmentUpdateEvent].
 *
 * The [GlobalSearchUpdateEventSource] class itself (flow merging) is covered by ViewModel
 * integration tests that construct it with a real viewModelScope.
 */
class GlobalSearchUpdateEventSourceTest {

    // ─── applyChannelMessageUpdateEvent: ChannelUpdated ──────────────────────────

    @Test
    fun `ChannelUpdated replaces matching ChannelItem with new channel data`() {
        val oldChannel = fakeChannel(id = 1L)
        val newChannel = fakeChannel(id = 1L)
        val unchanged = fakeChannel(id = 2L)
        val list = listOf(
            GlobalSearchListItem.ChannelItem(oldChannel),
            GlobalSearchListItem.ChannelItem(unchanged),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.ChannelUpdated(newChannel)
        )

        assertThat((result[0] as GlobalSearchListItem.ChannelItem).channel)
            .isSameInstanceAs(newChannel)
        assertThat((result[1] as GlobalSearchListItem.ChannelItem).channel)
            .isSameInstanceAs(unchanged)
    }

    @Test
    fun `ChannelUpdated leaves non-matching ChannelItems and MessageItems unchanged`() {
        val list = listOf(
            GlobalSearchListItem.ChannelItem(fakeChannel(id = 2L)),
            fakeMessageItem(id = 5L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.ChannelUpdated(fakeChannel(id = 99L))
        )

        assertThat(result).containsExactlyElementsIn(list).inOrder()
    }

    // ─── applyChannelMessageUpdateEvent: ChannelsDeleted ─────────────────────────

    @Test
    fun `ChannelsDeleted removes only the matching ChannelItems`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_chats),
            GlobalSearchListItem.ChannelItem(fakeChannel(id = 1L)),
            GlobalSearchListItem.ChannelItem(fakeChannel(id = 2L)),
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 10L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.ChannelsDeleted(listOf(1L))
        )

        val channelItems = result.filterIsInstance<GlobalSearchListItem.ChannelItem>()
        assertThat(channelItems).hasSize(1)
        assertThat(channelItems[0].channel.id).isEqualTo(2L)
    }

    @Test
    fun `ChannelsDeleted removes section header when all its channels are deleted`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_chats),
            GlobalSearchListItem.ChannelItem(fakeChannel(id = 1L)),
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 10L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.ChannelsDeleted(listOf(1L))
        )

        val headers = result.filterIsInstance<GlobalSearchListItem.SectionHeader>()
        assertThat(headers).hasSize(1)
        assertThat(headers[0].titleRes).isEqualTo(R.string.sceyt_messages)
    }

    @Test
    fun `ChannelsDeleted keeps section header when other channels remain under it`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_chats),
            GlobalSearchListItem.ChannelItem(fakeChannel(id = 1L)),
            GlobalSearchListItem.ChannelItem(fakeChannel(id = 2L)),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.ChannelsDeleted(listOf(1L))
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.SectionHeader>()).hasSize(1)
        assertThat(result.filterIsInstance<GlobalSearchListItem.ChannelItem>()).hasSize(1)
    }

    @Test
    fun `ChannelsDeleted with empty ids list leaves list unchanged`() {
        val list = listOf(
            GlobalSearchListItem.ChannelItem(fakeChannel(id = 1L)),
            fakeMessageItem(id = 5L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.ChannelsDeleted(emptyList())
        )

        assertThat(result).containsExactlyElementsIn(list).inOrder()
    }

    // ─── applyChannelMessageUpdateEvent: MessageUpdated (edit) ───────────────────

    @Test
    fun `MessageUpdated with non-deleted state replaces matching MessageItem`() {
        val newMessage = fakeMessage(id = 5L, state = MessageState.Edited)
        val list = listOf(
            fakeMessageItem(id = 5L),
            fakeMessageItem(id = 6L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(newMessage)
        )

        val updated = result.filterIsInstance<GlobalSearchListItem.MessageItem>()
            .first { it.result.message.id == 5L }
        assertThat(updated.result.message).isSameInstanceAs(newMessage)
    }

    @Test
    fun `MessageUpdated with non-deleted state does not affect other items`() {
        val newMessage = fakeMessage(id = 5L, state = MessageState.Edited)
        val other = fakeMessageItem(id = 6L)
        val list = listOf(fakeMessageItem(id = 5L), other)

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(newMessage)
        )

        assertThat(result[1]).isSameInstanceAs(other)
    }

    // ─── applyChannelMessageUpdateEvent: MessageUpdated (delete) ─────────────────

    @Test
    fun `MessageUpdated Deleted removes matching MessageItem`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 5L),
            fakeMessageItem(id = 6L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 5L, state = MessageState.Deleted))
        )

        val messageItems = result.filterIsInstance<GlobalSearchListItem.MessageItem>()
        assertThat(messageItems).hasSize(1)
        assertThat(messageItems[0].result.message.id).isEqualTo(6L)
    }

    @Test
    fun `MessageUpdated DeletedHard removes matching MessageItem`() {
        val list = listOf(fakeMessageItem(id = 7L), fakeMessageItem(id = 8L))

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 7L, state = MessageState.DeletedHard))
        )

        val messageItems = result.filterIsInstance<GlobalSearchListItem.MessageItem>()
        assertThat(messageItems).hasSize(1)
        assertThat(messageItems[0].result.message.id).isEqualTo(8L)
    }

    @Test
    fun `MessageUpdated Deleted removes section header when no messages remain`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_chats),
            GlobalSearchListItem.ChannelItem(fakeChannel(1L)),
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 5L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 5L, state = MessageState.Deleted))
        )

        val headers = result.filterIsInstance<GlobalSearchListItem.SectionHeader>()
        assertThat(headers).hasSize(1)
        assertThat(headers[0].titleRes).isEqualTo(R.string.sceyt_chats)
    }

    @Test
    fun `MessageUpdated Deleted keeps section header when other messages remain`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 5L),
            fakeMessageItem(id = 6L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 5L, state = MessageState.Deleted))
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.SectionHeader>()).hasSize(1)
        assertThat(result.filterIsInstance<GlobalSearchListItem.MessageItem>()).hasSize(1)
    }

    // ─── applyAttachmentUpdateEvent: ChannelUpdated ───────────────────────────────

    @Test
    fun `ChannelUpdated is a no-op for attachment lists`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.ChannelUpdated(fakeChannel(id = 10L))
        )

        assertThat(result).isSameInstanceAs(list)
    }

    // ─── applyAttachmentUpdateEvent: ChannelsDeleted ─────────────────────────────

    @Test
    fun `ChannelsDeleted removes matching AttachmentItems`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            fakeAttachmentItem(msgId = 2L, channelId = 20L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.ChannelsDeleted(listOf(10L))
        )

        val attachments = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()
        assertThat(attachments).hasSize(1)
        assertThat(attachments[0].result.channel.id).isEqualTo(20L)
    }

    @Test
    fun `ChannelsDeleted removes DateSeparator when all its attachments are removed`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            GlobalSearchListItem.DateSeparator(2000L),
            fakeAttachmentItem(msgId = 2L, channelId = 20L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.ChannelsDeleted(listOf(10L))
        )

        val separators = result.filterIsInstance<GlobalSearchListItem.DateSeparator>()
        assertThat(separators).hasSize(1)
        assertThat(separators[0].timestamp).isEqualTo(2000L)
    }

    @Test
    fun `ChannelsDeleted keeps DateSeparator when other attachments for that date remain`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            fakeAttachmentItem(msgId = 2L, channelId = 20L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.ChannelsDeleted(listOf(10L))
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.DateSeparator>()).hasSize(1)
        assertThat(result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()).hasSize(1)
    }

    // ─── applyAttachmentUpdateEvent: MessageUpdated ───────────────────────────────

    @Test
    fun `MessageUpdated Edited should for attachment lists`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 1L, state = MessageState.Edited))
        )

        assertThat(result).isNotSameInstanceAs(list)
    }

    @Test
    fun `MessageUpdated Deleted removes matching AttachmentItems`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            fakeAttachmentItem(msgId = 2L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 1L, state = MessageState.Deleted))
        )

        val attachments = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()
        assertThat(attachments).hasSize(1)
        assertThat(attachments[0].result.message.id).isEqualTo(2L)
    }

    @Test
    fun `MessageUpdated Deleted removes DateSeparator when all its attachments are gone`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            GlobalSearchListItem.DateSeparator(2000L),
            fakeAttachmentItem(msgId = 2L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 1L, state = MessageState.Deleted))
        )

        val separators = result.filterIsInstance<GlobalSearchListItem.DateSeparator>()
        assertThat(separators).hasSize(1)
        assertThat(separators[0].timestamp).isEqualTo(2000L)
    }

    @Test
    fun `MessageUpdated Deleted keeps DateSeparator when other attachments remain for that date`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            fakeAttachmentItem(msgId = 2L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessageUpdated(fakeMessage(id = 1L, state = MessageState.Deleted))
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.DateSeparator>()).hasSize(1)
        assertThat(result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()).hasSize(1)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private fun fakeChannel(id: Long): SceytChannel {
        val channel = mock<SceytChannel>()
        whenever(channel.id).thenReturn(id)
        return channel
    }

    private fun fakeMessage(id: Long, state: MessageState?): SceytMessage {
        val message = mock<SceytMessage>()
        whenever(message.id).thenReturn(id)
        if (state != null) whenever(message.state).thenReturn(state)
        return message
    }

    private fun fakeMessageItem(id: Long): GlobalSearchListItem.MessageItem {
        val result = GlobalSearchMessageResult(
            message = fakeMessage(id = id, state = null),
            channel = mock(),
        )
        return GlobalSearchListItem.MessageItem(result, query = "")
    }

    private fun fakeAttachmentItem(msgId: Long, channelId: Long): GlobalSearchListItem.AttachmentItem {
        val attachment = mock<SceytAttachment>()
        val result = GlobalSearchAttachmentResult(
            attachment = attachment,
            message = fakeMessage(id = msgId, state = null),
            channel = fakeChannel(id = channelId),
            sender = null,
            kind = GlobalSearchAttachmentKind.Media,
        )
        return GlobalSearchListItem.AttachmentItem(result, query = "")
    }
}
