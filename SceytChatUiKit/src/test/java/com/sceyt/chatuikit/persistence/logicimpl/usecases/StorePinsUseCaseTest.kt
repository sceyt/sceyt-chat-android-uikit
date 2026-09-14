package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.data.models.messages.SceytPinDetails
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity
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

class StorePinsUseCaseTest {

    private val messageDao = mock<MessageDao>()
    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val refreshPinnedMessageCache = mock<RefreshPinnedMessageCacheUseCase>()
    private val useCase = StorePinsUseCase(
        messageDao,
        pinnedMessageDao,
        refreshPinnedMessageCache,
    )

    @Test
    fun `a server pin prefers the local outgoing tid and stamps server ordering data`() = runTest {
        val pin = serverPin(
            pinId = 900L,
            message = sceytMessage(
                id = 42L,
                tid = 0L,
                pinDetails = SceytPinDetails(true, 5_000L, PinType.PERSONAL),
            ),
        )
        whenever(messageDao.getMessageById(42L)).thenReturn(messageDb(messageEntity(tid = 77L)))

        useCase(channelId = 7L, pins = listOf(pin))

        val entity = argumentCaptor<PinnedMessageEntity>()
        verify(pinnedMessageDao).upsertWithMirror(entity.capture())
        assertThat(entity.firstValue.messageTid).isEqualTo(77L)
        assertThat(entity.firstValue.serverPinId).isEqualTo(900L)
        assertThat(entity.firstValue.pinScope).isEqualTo(1)
        assertThat(entity.firstValue.pinnedUntil).isEqualTo(5_000L)
        assertThat(entity.firstValue.syncState).isEqualTo(PinSyncStateEntity.Synced.value)
        verifyBlocking(messageDao, never()) { upsertMessage(any()) }
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 77L) }
    }

    @Test
    fun `a server pin outside the local history is stored as an unlisted message`() = runTest {
        val pin = serverPin(
            message = sceytMessage(id = 42L, tid = 11L),
        )
        whenever(messageDao.getMessageById(42L)).thenReturn(null)

        useCase(channelId = 7L, pins = listOf(pin))

        val stored = argumentCaptor<com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb>()
        verifyBlocking(messageDao) { upsertMessage(stored.capture()) }
        assertThat(stored.firstValue.messageEntity.unList).isTrue()
        assertThat(stored.firstValue.messageEntity.tid).isEqualTo(11L)
    }

    @Test
    fun `server pins without a message id or for ephemeral messages are ignored`() = runTest {
        val missingId = serverPin(message = sceytMessage(id = 0L, tid = 11L))
        val transient = serverPin(message = sceytMessage(id = 2L, isTransient = true))
        val deleted = serverPin(message = sceytMessage(id = 3L, state = MessageState.Deleted))

        useCase(channelId = 7L, pins = listOf(missingId, transient, deleted))

        verify(pinnedMessageDao, never()).upsertWithMirror(any())
        verifyBlocking(messageDao, never()) { upsertMessage(any()) }
    }

    @Test
    fun `a server snapshot preserves a pending unpin and its cleared mirror`() = runTest {
        whenever(messageDao.getMessageById(42L)).thenReturn(messageDb())
        whenever(pinnedMessageDao.getByTid(42L, 7L))
            .thenReturn(pinnedEntity(syncState = PinSyncStateEntity.PendingUnpin.value))

        useCase(7L, listOf(sceytPinnedMessage()))

        verify(pinnedMessageDao, never()).upsertWithMirror(any())
    }

    @Test
    fun `a server snapshot preserves a pending pin with a different scope`() = runTest {
        whenever(messageDao.getMessageById(42L)).thenReturn(messageDb())
        whenever(pinnedMessageDao.getByTid(42L, 7L))
            .thenReturn(pinnedEntity(syncState = PinSyncStateEntity.PendingPin.value, pinScope = 1))

        useCase(7L, listOf(sceytPinnedMessage(scope = PinType.SHARED)))

        verify(pinnedMessageDao, never()).upsertWithMirror(any())
    }

    private fun serverPin(
        pinId: Long = 500L,
        message: com.sceyt.chatuikit.data.models.messages.SceytMessage,
    ): SceytPinnedMessage = sceytPinnedMessage(
        id = pinId,
        message = message,
        scope = message.pinDetails?.pinType ?: PinType.SHARED,
        pinnedUntil = message.pinDetails?.pinnedTill,
    )
}
