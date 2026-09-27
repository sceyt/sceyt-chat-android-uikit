package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnpinMessageUseCaseTest {

    private val messageDao = mock<MessageDao>()
    private val pendingPinDao = mock<PendingPinDao>()
    private val refreshPinnedMessageCache = mock<RefreshPinnedMessageCacheUseCase>()
    private val useCase = UnpinMessageUseCase(messageDao, pendingPinDao, refreshPinnedMessageCache)
    private val channelId = 7L

    @Before
    fun storeRequests() = runTest {
        doSuspendableAnswer { it.getArgument<PendingPinEntity>(0).copy(id = 2L) }
            .whenever(pendingPinDao).replace(any())
    }

    @Test
    fun `unpinning a confirmed pin stores an unpin request`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(pinnedMessage = pinnedEntity()))

        val result = useCase(channelId, messageTid = 42L)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        val captor = argumentCaptor<PendingPinEntity>()
        verify(pendingPinDao).replace(captor.capture())
        assertThat(captor.firstValue.isPin).isFalse()
        assertThat(captor.firstValue.messageId).isEqualTo(42L)
        verifyBlocking(refreshPinnedMessageCache) { invoke(any(), anyVararg()) }
    }

    @Test
    fun `unpinning a sent message with a pending pin stores an unpin request`() = runTest {
        // The pin may be in flight; the unpin replaces it and goes out after it.
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb(pendingPin = pendingPin()))

        useCase(channelId, messageTid = 42L)

        val captor = argumentCaptor<PendingPinEntity>()
        verify(pendingPinDao).replace(captor.capture())
        assertThat(captor.firstValue.isPin).isFalse()
    }

    @Test
    fun `unpinning a message that is not pinned is an error`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb())

        assertThat(useCase(channelId, 42L)).isInstanceOf(SceytResponse.Error::class.java)
        verify(pendingPinDao, never()).replace(any())
    }

    @Test
    fun `unpinning a message with a pending unpin is an error`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(
            messageDb(pinnedMessage = pinnedEntity(), pendingPin = pendingPin(isPin = false))
        )

        assertThat(useCase(channelId, 42L)).isInstanceOf(SceytResponse.Error::class.java)
    }
}
