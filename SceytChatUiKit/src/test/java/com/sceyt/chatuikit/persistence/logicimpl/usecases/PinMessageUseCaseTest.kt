package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
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
import kotlin.coroutines.ContinuationInterceptor

@RunWith(RobolectricTestRunner::class)
class PinMessageUseCaseTest {

    private val messageDao = mock<MessageDao>()
    private val pendingPinDao = mock<PendingPinDao>()
    private val dispatcher = StandardTestDispatcher()
    private val useCase = PinMessageUseCase(messageDao, pendingPinDao, mock(), dispatcher)
    private val channelId = 7L

    @Test
    fun `pin validation runs on the injected IO dispatcher`() = runTest(dispatcher) {
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val useCase = PinMessageUseCase(messageDao, pendingPinDao, mock(), ioDispatcher)
        doSuspendableAnswer {
            assertThat(currentCoroutineContext()[ContinuationInterceptor]).isSameInstanceAs(ioDispatcher)
            null
        }.whenever(messageDao).getMessageByTid(42L)

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
    }

    @Before
    fun storeRequests() = runTest(dispatcher) {
        doSuspendableAnswer { it.getArgument<PendingPinEntity>(0).copy(id = 1L) }
            .whenever(pendingPinDao).replace(any())
    }

    @Test
    fun `stores a pin request with the chosen scope`() = runTest(dispatcher) {
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
    fun `refuses to pin a pending message`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(99L)).thenReturn(
            messageDb(messageEntity(id = 0L, tid = 99L, deliveryStatus = MessageDeliveryStatus.Pending))
        )

        assertThat(useCase(channelId, 99L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `pinning again after a pending unpin replaces the unpin`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(
            messageDb(pinnedMessage = pinnedEntity(), pendingPin = pendingPin(isPin = false))
        )

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Success::class.java)
        verify(pendingPinDao).replace(any())
    }

    @Test
    fun `refuses to pin a message that is already pinned`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(pinnedMessage = pinnedEntity()))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin a message with a pending pin`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(pendingPin = pendingPin()))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin a view-once message`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(messageEntity(viewOnce = true)))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin a transient message`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(messageEntity(isTransient = true)))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `refuses to pin an auto-deleting message`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(messageEntity(autoDeleteAt = 5_000L)))

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `fails when the message is not stored`() = runTest(dispatcher) {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(null)

        assertThat(useCase(channelId, 42L, PinType.SHARED)).isInstanceOf(SceytResponse.Error::class.java)
    }
}
