package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.data.models.messages.SceytPinDetails
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
        whenever(messageDao.getMessageTidById(42L)).thenReturn(77L)

        useCase(channelId = 7L, pins = listOf(pin))

        val entities = argumentCaptor<List<PinnedMessageEntity>>()
        verify(pinnedMessageDao).insertAllIfMessagesExist(entities.capture())
        val entity = entities.firstValue.single()
        assertThat(entity.messageTid).isEqualTo(77L)
        assertThat(entity.serverPinId).isEqualTo(900L)
        assertThat(entity.pinScope).isEqualTo(1)
        assertThat(entity.pinnedUntil).isEqualTo(5_000L)
        verifyBlocking(messageDao, never()) { upsertMessage(any()) }
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 77L) }
    }

    @Test
    fun `a server pin outside the local history is stored as an unlisted message`() = runTest {
        val pin = serverPin(
            message = sceytMessage(id = 42L, tid = 11L),
        )
        useCase(channelId = 7L, pins = listOf(pin))

        val stored = argumentCaptor<MessageDb>()
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

        verify(pinnedMessageDao, never()).insertAllIfMessagesExist(any())
        verifyBlocking(messageDao, never()) { upsertMessage(any()) }
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
