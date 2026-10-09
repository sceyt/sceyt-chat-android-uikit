package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SendPendingPinsUseCaseTest {

    private val pendingPinDao = mock<PendingPinDao>()
    private val sendPinUseCase = mock<SendPinUseCase>()
    private val sendUnpinUseCase = mock<SendUnpinUseCase>()
    private val useCase = SendPendingPinsUseCase(pendingPinDao, sendPinUseCase, sendUnpinUseCase)

    @Test
    fun `each request goes out through its own use case`() = runTest {
        val pin = pendingPin(messageTid = 1L, id = 1L)
        val unpin = pendingPin(messageTid = 2L, id = 2L, isPin = false)
        whenever(pendingPinDao.getAll()).thenReturn(listOf(pin, unpin))

        useCase()

        verify(sendPinUseCase).invoke(pin)
        verify(sendUnpinUseCase).invoke(unpin)
    }

    @Test
    fun `a channel sends only its own requests`() = runTest {
        whenever(pendingPinDao.getByChannel(7L)).thenReturn(emptyList())

        useCase(channelId = 7L)

        verify(pendingPinDao, never()).getAll()
        verify(sendPinUseCase, never()).invoke(any())
        verify(sendUnpinUseCase, never()).invoke(any())
    }
}
