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
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.coroutines.CoroutineContext

/**
 * Tests for the pure list-transformation extension functions in GlobalSearchUpdateSource.kt:
 * [applyChannelMessageUpdateEvent] and [applyAttachmentUpdateEvent].
 *
 * The [GlobalSearchUpdateEventSource] class itself (flow merging) is covered by ViewModel
 * integration tests that construct it with a real viewModelScope.
 */
class GlobalSearchUpdateEventSourceTest {
    private val dispatcher = StandardTestDispatcher()
    private val fileTransferService = mock<FileTransferService>()
    private val attachmentLogic = mock<PersistenceAttachmentLogic>()
    private val fileChecksumDao = mock<FileChecksumDao>()

    @Before
    fun setUp() {
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { fileTransferService }
                single<PersistenceAttachmentLogic> { attachmentLogic }
                single<FileChecksumDao> { fileChecksumDao }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { dispatcher }
            })
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
    }

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

    // ─── applyChannelMessageUpdateEvent: MessagesUpdated (edit) ──────────────────

    @Test
    fun `MessagesUpdated with non-deleted state replaces matching MessageItem`() {
        val newMessage = fakeMessage(id = 5L, state = MessageState.Edited)
        val list = listOf(
            fakeMessageItem(id = 5L),
            fakeMessageItem(id = 6L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(newMessage))
        )

        val updated = result.filterIsInstance<GlobalSearchListItem.MessageItem>()
            .first { it.result.message.id == 5L }
        assertThat(updated.result.message).isSameInstanceAs(newMessage)
    }

    @Test
    fun `MessagesUpdated with non-deleted state does not affect other items`() {
        val newMessage = fakeMessage(id = 5L, state = MessageState.Edited)
        val other = fakeMessageItem(id = 6L)
        val list = listOf(fakeMessageItem(id = 5L), other)

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(newMessage))
        )

        assertThat(result[1]).isSameInstanceAs(other)
    }

    // ─── applyChannelMessageUpdateEvent: MessagesUpdated (delete) ────────────────

    @Test
    fun `MessagesUpdated Deleted removes matching MessageItem`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 5L),
            fakeMessageItem(id = 6L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 5L, state = MessageState.Deleted)))
        )

        val messageItems = result.filterIsInstance<GlobalSearchListItem.MessageItem>()
        assertThat(messageItems).hasSize(1)
        assertThat(messageItems[0].result.message.id).isEqualTo(6L)
    }

    @Test
    fun `MessagesUpdated DeletedHard removes matching MessageItem`() {
        val list = listOf(fakeMessageItem(id = 7L), fakeMessageItem(id = 8L))

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 7L, state = MessageState.DeletedHard)))
        )

        val messageItems = result.filterIsInstance<GlobalSearchListItem.MessageItem>()
        assertThat(messageItems).hasSize(1)
        assertThat(messageItems[0].result.message.id).isEqualTo(8L)
    }

    @Test
    fun `MessagesUpdated Deleted removes section header when no messages remain`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_chats),
            GlobalSearchListItem.ChannelItem(fakeChannel(1L)),
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 5L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 5L, state = MessageState.Deleted)))
        )

        val headers = result.filterIsInstance<GlobalSearchListItem.SectionHeader>()
        assertThat(headers).hasSize(1)
        assertThat(headers[0].titleRes).isEqualTo(R.string.sceyt_chats)
    }

    @Test
    fun `MessagesUpdated Deleted keeps section header when other messages remain`() {
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 5L),
            fakeMessageItem(id = 6L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 5L, state = MessageState.Deleted)))
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.SectionHeader>()).hasSize(1)
        assertThat(result.filterIsInstance<GlobalSearchListItem.MessageItem>()).hasSize(1)
    }

    @Test
    fun `MessagesUpdated batch on message list - some deleted some edited - handled in single event`() {
        val editedMessage = fakeMessage(id = 10L, state = MessageState.Edited)
        val list = listOf(
            GlobalSearchListItem.SectionHeader(R.string.sceyt_messages),
            fakeMessageItem(id = 10L),
            fakeMessageItem(id = 11L),
            fakeMessageItem(id = 12L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(
                listOf(
                    editedMessage,
                    fakeMessage(id = 11L, state = MessageState.Deleted),
                )
            )
        )

        val messageItems = result.filterIsInstance<GlobalSearchListItem.MessageItem>()
        assertThat(messageItems).hasSize(2)
        assertThat(messageItems.first { it.result.message.id == 10L }.result.message)
            .isSameInstanceAs(editedMessage)
        assertThat(messageItems.any { it.result.message.id == 11L }).isFalse()
        assertThat(messageItems.any { it.result.message.id == 12L }).isTrue()
    }

    @Test
    fun `MessagesUpdated empty list leaves list unchanged`() {
        val list = listOf(
            fakeMessageItem(id = 1L),
            fakeMessageItem(id = 2L),
        )

        val result = list.applyChannelMessageUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(emptyList())
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.MessageItem>())
            .hasSize(2)
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

    // ─── applyAttachmentUpdateEvent: MessagesUpdated ─────────────────────────────

    @Test
    fun `MessagesUpdated Edited updates message body in matching AttachmentItem`() {
        val newMessage = fakeMessage(id = 1L, state = MessageState.Edited)
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(newMessage))
        )

        val updated = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>().first()
        assertThat(updated.result.message).isSameInstanceAs(newMessage)
    }

    @Test
    fun `MessagesUpdated Deleted removes matching AttachmentItems`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            fakeAttachmentItem(msgId = 2L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 1L, state = MessageState.Deleted)))
        )

        val attachments = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()
        assertThat(attachments).hasSize(1)
        assertThat(attachments[0].result.message.id).isEqualTo(2L)
    }

    @Test
    fun `MessagesUpdated DeletedHard removes matching AttachmentItems`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 3L, channelId = 10L),
            fakeAttachmentItem(msgId = 4L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 3L, state = MessageState.DeletedHard)))
        )

        val attachments = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()
        assertThat(attachments).hasSize(1)
        assertThat(attachments[0].result.message.id).isEqualTo(4L)
    }

    @Test
    fun `MessagesUpdated Deleted removes DateSeparator when all its attachments are gone`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            GlobalSearchListItem.DateSeparator(2000L),
            fakeAttachmentItem(msgId = 2L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 1L, state = MessageState.Deleted)))
        )

        val separators = result.filterIsInstance<GlobalSearchListItem.DateSeparator>()
        assertThat(separators).hasSize(1)
        assertThat(separators[0].timestamp).isEqualTo(2000L)
    }

    @Test
    fun `MessagesUpdated Deleted keeps DateSeparator when other attachments remain for that date`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
            fakeAttachmentItem(msgId = 2L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(listOf(fakeMessage(id = 1L, state = MessageState.Deleted)))
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.DateSeparator>()).hasSize(1)
        assertThat(result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()).hasSize(1)
    }

    @Test
    fun `MessagesUpdated batch on attachment list - some deleted some edited - handled in single event`() {
        val editedMessage = fakeMessage(id = 10L, state = MessageState.Edited)
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 10L, channelId = 5L),
            fakeAttachmentItem(msgId = 11L, channelId = 5L),
            fakeAttachmentItem(msgId = 12L, channelId = 5L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(
                listOf(
                    editedMessage,
                    fakeMessage(id = 11L, state = MessageState.Deleted),
                )
            )
        )

        val attachments = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()
        assertThat(attachments).hasSize(2)
        assertThat(attachments.first { it.result.message.id == 10L }.result.message)
            .isSameInstanceAs(editedMessage)
        assertThat(attachments.any { it.result.message.id == 11L }).isFalse()
        assertThat(attachments.any { it.result.message.id == 12L }).isTrue()
    }

    @Test
    fun `MessagesUpdated empty list leaves attachment list unchanged`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(msgId = 1L, channelId = 10L),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.MessagesUpdated(emptyList())
        )

        assertThat(result.filterIsInstance<GlobalSearchListItem.AttachmentItem>()).hasSize(1)
    }

    @Test
    fun `TransferUpdated Downloaded updates matching attachment state and file path`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(
                msgId = 1L,
                channelId = 10L,
                attachment = fakeAttachment(
                    messageTid = 101L,
                    filePath = null,
                    transferState = TransferState.PendingDownload,
                    progressPercent = 0f,
                    url = "remote-url"
                )
            ),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.TransferUpdated(
                TransferData(
                    messageTid = 101L,
                    progressPercent = 100f,
                    state = TransferState.Downloaded,
                    filePath = "/tmp/downloaded.pdf",
                    url = "remote-url"
                )
            )
        )

        val updated = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>().first()
        assertThat(updated.result.attachment.transferState).isEqualTo(TransferState.Downloaded)
        assertThat(updated.result.attachment.progressPercent).isEqualTo(100f)
        assertThat(updated.result.attachment.filePath).isEqualTo("/tmp/downloaded.pdf")
    }

    @Test
    fun `TransferUpdated keeps existing url when incoming url is missing`() {
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            fakeAttachmentItem(
                msgId = 1L,
                channelId = 10L,
                attachment = fakeAttachment(
                    messageTid = 101L,
                    filePath = "/tmp/downloaded.pdf",
                    transferState = TransferState.Downloaded,
                    progressPercent = 100f,
                    url = "remote-url"
                )
            ),
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.TransferUpdated(
                TransferData(
                    messageTid = 101L,
                    progressPercent = 42f,
                    state = TransferState.ErrorDownload,
                    filePath = null,
                    url = null
                )
            )
        )

        val updated = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>().first()
        assertThat(updated.result.attachment.transferState).isEqualTo(TransferState.ErrorDownload)
        assertThat(updated.result.attachment.progressPercent).isEqualTo(42f)
        assertThat(updated.result.attachment.filePath).isNull()
        assertThat(updated.result.attachment.url).isEqualTo("remote-url")
    }

    @Test
    fun `TransferUpdated ThumbLoaded applies transfer data to attachment`() {
        val attachmentItem = fakeAttachmentItem(
            msgId = 1L,
            channelId = 10L,
            attachment = fakeAttachment(
                messageTid = 101L,
                filePath = "/tmp/downloaded.pdf",
                transferState = TransferState.Downloaded,
                progressPercent = 100f
            )
        )
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            attachmentItem,
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.TransferUpdated(
                TransferData(
                    messageTid = 101L,
                    progressPercent = 100f,
                    state = TransferState.ThumbLoaded,
                    filePath = "/tmp/thumb.jpg",
                    url = null
                )
            )
        )

        val updated = result.filterIsInstance<GlobalSearchListItem.AttachmentItem>().first()
        assertThat(updated.result.attachment.filePath).isEqualTo("/tmp/thumb.jpg")
        assertThat(updated.result.attachment.transferState).isEqualTo(TransferState.ThumbLoaded)
        assertThat(updated).isNotSameInstanceAs(attachmentItem)
    }

    @Test
    fun `TransferUpdated for non matching messageTid keeps attachment list content unchanged`() {
        val attachmentItem = fakeAttachmentItem(
            msgId = 1L,
            channelId = 10L,
            attachment = fakeAttachment(messageTid = 101L)
        )
        val list = listOf(
            GlobalSearchListItem.DateSeparator(1000L),
            attachmentItem,
        )

        val result = list.applyAttachmentUpdateEvent(
            GlobalSearchUpdateEvent.TransferUpdated(
                TransferData(
                    messageTid = 202L,
                    progressPercent = 100f,
                    state = TransferState.Downloaded,
                    filePath = "/tmp/downloaded.pdf",
                    url = null
                )
            )
        )

        assertThat(result).containsExactlyElementsIn(list).inOrder()
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

    private fun fakeAttachment(
        messageTid: Long,
        filePath: String? = null,
        transferState: TransferState? = TransferState.PendingDownload,
        progressPercent: Float? = 0f,
        url: String? = null,
        createdAt: Long = 1000L,
    ) = SceytAttachment(
        id = messageTid,
        messageId = messageTid,
        messageTid = messageTid,
        userId = null,
        name = "file-$messageTid",
        type = "file",
        metadata = null,
        fileSize = 128L,
        createdAt = createdAt,
        url = url,
        filePath = filePath,
        transferState = transferState,
        progressPercent = progressPercent,
        originalFilePath = null,
        linkPreviewDetails = null
    )

    private fun fakeAttachmentItem(
        msgId: Long,
        channelId: Long,
        attachment: SceytAttachment = fakeAttachment(messageTid = msgId, createdAt = 1000L),
    ): GlobalSearchListItem.AttachmentItem {
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
