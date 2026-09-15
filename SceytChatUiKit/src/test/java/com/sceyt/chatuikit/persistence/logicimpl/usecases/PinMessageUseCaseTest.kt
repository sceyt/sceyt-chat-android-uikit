package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity.Companion.UNKNOWN_SERVER_PIN_ID
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
class PinMessageUseCaseTest {

    private val messageDao = mock<MessageDao>()
    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val pinRepository = mock<PinRepository>()

    private val useCase = PinMessageUseCase(
        messageDao = messageDao,
        pinnedMessageDao = pinnedMessageDao,
        sendPendingPinsUseCase = SendPendingPinsUseCase(
            pinnedMessageDao, pinRepository, ConfirmPinUseCase(pinnedMessageDao), mock(), mock(),
        ),
        refreshPinnedMessageCache = mock(),
    )

    @org.junit.Before
    fun trackStoredPin() = runTest {
        org.mockito.kotlin.doSuspendableAnswer { call ->
            val entity = call.getArgument<PinnedMessageEntity>(0)
            whenever(pinnedMessageDao.getByTid(entity.messageTid, entity.channelId)).thenReturn(entity)
            Unit
        }.whenever(pinnedMessageDao) { upsertWithMirror(any()) }
    }

    private val channelId = 7L

    @Test
    fun `writes the optimistic row as a pending intent before contacting the server`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb())
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Error(null))

        useCase(channelId, messageTid = 42L, pinType = PinType.SHARED)

        val captor = argumentCaptor<PinnedMessageEntity>()
        verify(pinnedMessageDao).upsertWithMirror(captor.capture())
        with(captor.firstValue) {
            assertThat(syncState).isEqualTo(PinSyncStateEntity.PendingPin.value)
            // Sorts to the newest-pin end so an optimistic pin appends rather than
            // jumping to the head of the banner.
            assertThat(serverPinId).isEqualTo(UNKNOWN_SERVER_PIN_ID)
        }
    }

    @Test
    fun `keeps the pending row when the server call fails`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb())
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Error(null))

        val result = useCase(channelId, messageTid = 42L, pinType = PinType.SHARED)

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        // Never rolled back — the row stays a durable intent for the reconnect flush.
        verify(pinnedMessageDao, never()).deleteWithMirror(any(), any())
        verifyBlocking(pinnedMessageDao) { incrementRetry(any(), any(), any()) }
    }

    @Test
    fun `refuses to pin a view-once message`() = runTest {
        whenever(messageDao.getMessageByTid(42L))
            .thenReturn(messageDb(messageEntity(viewOnce = true)))

        val result = useCase(channelId, messageTid = 42L, pinType = PinType.SHARED)

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        verify(pinnedMessageDao, never()).upsertWithMirror(any())
        verifyBlocking(pinRepository, never()) { pinMessages(any(), any(), any(), anyOrNull()) }
    }

    @Test
    fun `refuses to pin a transient message`() = runTest {
        whenever(messageDao.getMessageByTid(42L))
            .thenReturn(messageDb(messageEntity(isTransient = true)))

        assertThat(useCase(channelId, 42L, PinType.SHARED))
            .isInstanceOf(SceytResponse.Error::class.java)
        verify(pinnedMessageDao, never()).upsertWithMirror(any())
    }

    @Test
    fun `refuses to pin an auto-deleting message`() = runTest {
        whenever(messageDao.getMessageByTid(42L))
            .thenReturn(messageDb(messageEntity(autoDeleteAt = 5_000L)))

        assertThat(useCase(channelId, 42L, PinType.SHARED))
            .isInstanceOf(SceytResponse.Error::class.java)
        verify(pinnedMessageDao, never()).upsertWithMirror(any())
    }

    @Test
    fun `refuses to pin the same message twice`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb())
        whenever(pinnedMessageDao.getByTid(42L, channelId))
            .thenReturn(pinnedEntity(syncState = PinSyncStateEntity.Synced.value))

        val result = useCase(channelId, messageTid = 42L, pinType = PinType.SHARED)

        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        verify(pinnedMessageDao, never()).upsertWithMirror(any())
    }

    @Test
    fun `does not contact the server for a message with no server id yet`() = runTest {
        whenever(messageDao.getMessageByTid(99L)).thenReturn(
            messageDb(
                messageEntity(
                    id = 0L,
                    tid = 99L,
                    deliveryStatus = MessageDeliveryStatus.Pending
                )
            )
        )

        useCase(channelId, messageTid = 99L, pinType = PinType.SHARED)

        // The row is still written, so the reconnect flush sends it once the send ack has
        // promoted tid -> id.
        verify(pinnedMessageDao).upsertWithMirror(any())
        verifyBlocking(pinRepository, never()) { pinMessages(any(), any(), any(), anyOrNull()) }
    }
}
