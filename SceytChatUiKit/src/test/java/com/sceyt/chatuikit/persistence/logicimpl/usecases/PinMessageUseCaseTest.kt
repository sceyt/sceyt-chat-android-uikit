package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinMessageUseCaseTest {

    private val messageDao = mock<MessageDao>()
    private val pendingPinDao = mock<PendingPinDao>()
    private val useCase = PinMessageUseCase(messageDao, pendingPinDao, mock())
    private val channelId = 7L

    @Before
    fun storeRequests() = runTest {
        doSuspendableAnswer { it.getArgument<PendingPinEntity>(0).copy(id = 1L) }
            .whenever(pendingPinDao).replace(any())
    }

    @Test
    fun `stores a pin request with the chosen scope`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb())

        val result = useCase(channelId, messageTid = 42L, pinType = PinType.PERSONAL)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        val captor = argumentCaptor<PendingPinEntity>()
        verify(pendingPinDao).replace(captor.capture())
        with(captor.firstValue) {
            assertThat(isPin).isTrue()
            assertThat(messageTid).isEqualTo(42L)
            assertThat(channelId).isEqualTo(7L)
            assertThat(messageId).isEqualTo(42L)
            assertThat(pinScope).isEqualTo(StoredPinScope.ForMe.value)
        }
    }

    @Test
    fun `refuses to pin a pending message`() = runTest {
        whenever(messageDao.getMessageByTid(99L)).thenReturn(
            messageDb(messageEntity(id = 0L, tid = 99L, deliveryStatus = MessageDeliveryStatus.Pending))
        )

        assertThat(useCase(channelId, 99L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `pinning again after a pending unpin replaces the unpin`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(
            messageDb(pinnedMessage = pinnedEntity(), pendingPin = pendingPin(isPin = false))
        )

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Success::class.java)
        verify(pendingPinDao).replace(any())
    }

    @Test
    fun `refuses to pin a message that is already pinned`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(pinnedMessage = pinnedEntity()))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin a message with a pending pin`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(pendingPin = pendingPin()))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin a view-once message`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(messageEntity(viewOnce = true)))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin a transient message`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(messageEntity(isTransient = true)))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin an auto-deleting message`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(messageEntity(autoDeleteAt = 5_000L)))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `fails when the message is not stored`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(null)

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
    }
}
