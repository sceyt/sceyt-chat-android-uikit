package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

class UpdatePinnedMessagesUseCaseTest {

    private val messageDao = mock<MessageDao>()
    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val refreshPinnedMessageCache = mock<RefreshPinnedMessageCacheUseCase>()
    private val useCase = UpdatePinnedMessagesUseCase(
        messageDao,
        pinnedMessageDao,
        refreshPinnedMessageCache,
        StorePinsUseCase(messageDao, pinnedMessageDao, refreshPinnedMessageCache),
    )

    @Test
    fun `a realtime pin uses the local tid when the server did not echo it`() = runTest {
        val pin = sceytPinnedMessage(
            id = 900L,
            message = sceytMessage(id = 42L, tid = 0L),
            messageTid = 0L,
            scope = PinType.PERSONAL,
            pinnedUntil = 5_000L,
        )
        whenever(messageDao.getMessageById(42L)).thenReturn(messageDb(messageEntity(tid = 77L)))

        useCase(PinUpdateEvent.Pinned(channelId = 7L, messages = listOf(pin)))

        val entity = argumentCaptor<PinnedMessageEntity>()
        verify(pinnedMessageDao).upsertWithMirror(entity.capture())
        assertThat(entity.firstValue.messageTid).isEqualTo(77L)
        assertThat(entity.firstValue.serverPinId).isEqualTo(900L)
        assertThat(entity.firstValue.pinScope).isEqualTo(1)
        assertThat(entity.firstValue.pinnedUntil).isEqualTo(5_000L)
        assertThat(entity.firstValue.syncState).isEqualTo(PinSyncState.Synced.value)
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 77L) }
        verifyBlocking(messageDao, never()) { upsertMessage(any()) }
    }

    @Test
    fun `a realtime pin stores a missing message outside the conversation list`() = runTest {
        val pin = sceytPinnedMessage(
            id = 901L,
            message = sceytMessage(id = 42L, tid = 11L),
            messageTid = 11L,
        )
        whenever(messageDao.getMessageById(42L)).thenReturn(null)

        useCase(PinUpdateEvent.Pinned(channelId = 7L, messages = listOf(pin)))

        val storedMessage = argumentCaptor<com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb>()
        verifyBlocking(messageDao) { upsertMessage(storedMessage.capture()) }
        assertThat(storedMessage.firstValue.messageEntity.unList).isTrue()
        assertThat(storedMessage.firstValue.messageEntity.tid).isEqualTo(11L)
    }

    @Test
    fun `a realtime pin ignores an ephemeral or deleted message`() = runTest {
        val ephemeral = sceytPinnedMessage(
            message = sceytMessage(id = 1L, isTransient = true),
        )
        val deleted = sceytPinnedMessage(
            message = sceytMessage(id = 2L, state = MessageState.Deleted),
        )

        useCase(PinUpdateEvent.Pinned(channelId = 7L, messages = listOf(ephemeral, deleted)))

        verify(pinnedMessageDao, never()).upsertWithMirror(any())
        verifyBlocking(messageDao, never()) { upsertMessage(any()) }
    }

    @Test
    fun `a realtime unpin removes both the durable row and its message mirror`() = runTest {
        val first = sceytPinnedMessage(message = sceytMessage(id = 1L, tid = 11L))
        val second = sceytPinnedMessage(message = sceytMessage(id = 2L, tid = 12L))

        useCase(PinUpdateEvent.Unpinned(channelId = 7L, messages = listOf(first, second)))

        verify(pinnedMessageDao).deleteWithMirror(11L, 7L)
        verify(pinnedMessageDao).deleteWithMirror(12L, 7L)
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 11L) }
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 12L) }
    }

    @Test
    fun `a realtime unpin resolves an outgoing message whose tid was omitted`() = runTest {
        val pin = sceytPinnedMessage(message = sceytMessage(id = 42L, tid = 0L))
        whenever(messageDao.getMessageById(42L)).thenReturn(messageDb(messageEntity(tid = 77L)))

        useCase(PinUpdateEvent.Unpinned(7L, listOf(pin)))

        verify(pinnedMessageDao).deleteWithMirror(77L, 7L)
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 77L) }
        verify(pinnedMessageDao, never()).deleteWithMirror(0L, 7L)
    }

    @Test
    fun `a realtime pin does not overwrite a queued unpin`() = runTest {
        whenever(messageDao.getMessageById(42L)).thenReturn(messageDb())
        whenever(pinnedMessageDao.getByTid(42L, 7L))
            .thenReturn(pinnedEntity(syncState = PinSyncState.PendingUnpin.value))

        useCase(PinUpdateEvent.Pinned(7L, listOf(sceytPinnedMessage())))

        verify(pinnedMessageDao, never()).upsertWithMirror(any())
    }

    @Test
    fun `a realtime unpin does not discard a queued local pin`() = runTest {
        whenever(messageDao.getMessageById(42L)).thenReturn(messageDb())
        whenever(pinnedMessageDao.getByTid(42L, 7L))
            .thenReturn(pinnedEntity(syncState = PinSyncState.PendingPin.value))

        useCase(PinUpdateEvent.Unpinned(7L, listOf(sceytPinnedMessage())))

        verify(pinnedMessageDao, never()).deleteWithMirror(any(), any())
    }

}
