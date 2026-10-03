package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SendPinUseCaseTest {

    private val pendingPinDao = mock<PendingPinDao>()
    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val pinRepository = mock<PinRepository>()
    private val systemMessageSender = mock<SystemMessageSender>()
    private val useCase = SendPinUseCase(
        pendingPinDao,
        pinnedMessageDao,
        pinRepository,
        systemMessageSender,
        mock(),
    )
    private val row = pendingPin(messageTid = 42L, id = 1L, messageId = 142L)

    /** 1 when the request was still stored, so the reply settled it; 0 when it was replaced. */
    private suspend fun givenSettled(settled: Boolean) {
        whenever(pendingPinDao.deleteById(1L)).thenReturn(if (settled) 1 else 0)
    }

    private suspend fun givenServerReplies(response: SceytResponse<List<SceytPinnedMessage>>) {
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull())).thenReturn(response)
    }

    private fun pin(scope: PinType = PinType.SHARED) =
        sceytPinnedMessage(id = 500L, message = sceytMessage(id = 142L, tid = 42L), scope = scope)

    @Test
    fun `sends the stored message id with its scope`() = runTest {
        val personal = row.copy(pinScope = StoredPinScope.ForMe.value)
        givenSettled(true)
        givenServerReplies(SceytResponse.Success(emptyList()))

        useCase(personal)

        verifyBlocking(pinRepository) { pinMessages(eq(7L), eq(listOf(142L)), eq(PinType.PERSONAL), anyOrNull()) }
    }

    @Test
    fun `a confirmed shared pin is stored, settled and announced`() = runTest {
        givenSettled(true)
        givenServerReplies(SceytResponse.Success(listOf(pin())))

        val result = useCase(row)

        assertThat((result as SceytResponse.Success).data?.messageId).isEqualTo(142L)
        val captor = argumentCaptor<PinnedMessageEntity>()
        verify(pinnedMessageDao).insertIfMessageExists(captor.capture())
        assertThat(captor.firstValue.messageTid).isEqualTo(42L)
        assertThat(captor.firstValue.serverPinId).isEqualTo(500L)
        verifyBlocking(systemMessageSender, times(1)) { sendMessagePinned(eq(7L), argThat { id == 142L }) }
    }

    @Test
    fun `a personal pin is not announced`() = runTest {
        givenSettled(true)
        givenServerReplies(SceytResponse.Success(listOf(pin(PinType.PERSONAL))))

        useCase(row.copy(pinScope = StoredPinScope.ForMe.value))

        verifyBlocking(systemMessageSender, never()) { sendMessagePinned(any(), any()) }
    }

    @Test
    fun `a pin the user replaced while it was in flight is not announced or returned`() = runTest {
        givenSettled(false)
        givenServerReplies(SceytResponse.Success(listOf(pin())))

        val result = useCase(row)

        assertThat((result as SceytResponse.Success).data).isNull()
        verifyBlocking(systemMessageSender, never()) { sendMessagePinned(any(), any()) }
    }

    @Test
    fun `a rejected pin is dropped and returned`() = runTest {
        givenServerReplies(SceytResponse.Error(mock<SceytException> { on { type }.thenReturn("NotAllowed") }))

        assertThat(useCase(row)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao).deleteById(1L)
    }

    @Test
    fun `a pin that failed for a resendable reason stays pending`() = runTest {
        givenServerReplies(SceytResponse.Error(null))

        assertThat(useCase(row)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).deleteById(any())
    }
}
