package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SendUnpinUseCaseTest {

    private val pendingPinDao = mock<PendingPinDao>()
    private val pinRepository = mock<PinRepository>()
    private val useCase = SendUnpinUseCase(pendingPinDao, pinRepository, mock())
    private val row = pendingPin(messageTid = 42L, id = 1L, messageId = 142L, isPin = false)

    private suspend fun givenServerReplies(response: SceytResponse<List<SceytPinnedMessage>>) {
        whenever(pinRepository.unpinMessages(any(), any())).thenReturn(response)
    }

    @Test
    fun `a confirmed unpin settles the request`() = runTest {
        givenServerReplies(SceytResponse.Success(emptyList()))

        val result = useCase(row)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        verifyBlocking(pinRepository) { unpinMessages(7L, listOf(142L)) }
        verify(pendingPinDao).settleUnpin(row)
    }

    @Test
    fun `unpinning a message the server does not have pinned counts as done`() = runTest {
        givenServerReplies(SceytResponse.Error(mock<SceytException> { on { type }.thenReturn("NotFound") }))

        val result = useCase(row)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        verify(pendingPinDao).settleUnpin(row)
    }

    @Test
    fun `a rejected unpin is dropped and returned`() = runTest {
        givenServerReplies(SceytResponse.Error(mock<SceytException> { on { type }.thenReturn("NotAllowed") }))

        assertThat(useCase(row)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao).deleteById(1L)
        verify(pendingPinDao, never()).settleUnpin(any())
    }

    @Test
    fun `an unpin that failed for a resendable reason stays pending`() = runTest {
        givenServerReplies(SceytResponse.Error(null))

        assertThat(useCase(row)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).deleteById(any())
    }
}
