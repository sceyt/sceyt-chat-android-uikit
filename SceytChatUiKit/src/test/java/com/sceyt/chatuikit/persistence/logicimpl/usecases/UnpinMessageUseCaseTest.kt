package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.times
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
class UnpinMessageUseCaseTest {

    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val pinRepository = mock<PinRepository>()
    private val refreshPinnedMessageCache = mock<RefreshPinnedMessageCacheUseCase>()
    private val useCase =
        UnpinMessageUseCase(
            pinnedMessageDao,
            SendPendingPinsUseCase(pinnedMessageDao, pinRepository, mock(), mock(), refreshPinnedMessageCache),
            refreshPinnedMessageCache,
        )

    @org.junit.Before
    fun trackPendingUnpin() = runTest {
        org.mockito.kotlin.doSuspendableAnswer { call ->
            val tid = call.getArgument<Long>(0)
            val channel = call.getArgument<Long>(1)
            val now = call.getArgument<Long>(2)
            val current = pinnedMessageDao.getByTid(tid, channel)!!
            whenever(pinnedMessageDao.getByTid(tid, channel)).thenReturn(current.copy(
                syncState = PinSyncState.PendingUnpin.value, lastAttemptAt = now
            ))
            Unit
        }.whenever(pinnedMessageDao) { markPendingUnpinWithMirror(any(), any(), any()) }
    }

    private val channelId = 7L

    @Test
    fun `unpin of an unsent message makes no server call`() = runTest {
        // A message without a server ID could not have been pinned remotely.
        whenever(pinnedMessageDao.getByTid(42L, channelId))
            .thenReturn(pinnedEntity(messageId = 0L, syncState = PinSyncState.PendingPin.value))

        val result = useCase(channelId, messageTid = 42L)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        verify(pinnedMessageDao).deleteWithMirror(42L, channelId)
        verifyBlocking(pinRepository, never()) { unpinMessages(any(), any()) }
    }

    @Test
    fun `unpinning a synced pin keeps the row as a pending intent until the server acks`() =
        runTest {
            whenever(pinnedMessageDao.getByTid(42L, channelId))
                .thenReturn(pinnedEntity(syncState = PinSyncState.Synced.value))
            whenever(pinRepository.unpinMessages(any(), any()))
                .thenReturn(SceytResponse.Error(null))

            val result = useCase(channelId, messageTid = 42L)

            assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
            // The mirror is cleared immediately so the bubble loses its pin right away, but
            // the row survives for the reconnect flush.
            verify(pinnedMessageDao).markPendingUnpinWithMirror(eq42(), eq7(), any())
            verify(pinnedMessageDao, never()).deleteWithMirror(any(), any())
        }

    @Test
    fun `drops the row once the server acknowledges the unpin`() = runTest {
        whenever(pinnedMessageDao.getByTid(42L, channelId))
            .thenReturn(pinnedEntity(syncState = PinSyncState.Synced.value))
        whenever(pinRepository.unpinMessages(any(), any()))
            .thenReturn(SceytResponse.Success(emptyList()))

        val result = useCase(channelId, messageTid = 42L)

        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        verify(pinnedMessageDao).deleteWithMirror(42L, channelId)
    }

    @Test
    fun `the conversation copy is refreshed as soon as the unpin is acknowledged`() = runTest {
        whenever(pinnedMessageDao.getByTid(42L, channelId))
            .thenReturn(pinnedEntity(syncState = PinSyncState.Synced.value))
        whenever(pinRepository.unpinMessages(any(), any()))
            .thenReturn(SceytResponse.Success(emptyList()))

        useCase(channelId, messageTid = 42L)

        // Twice: once for the optimistic clear, once after the server acknowledges the
        // removal. Missing the second one left the bubble pinned until the channel was
        // reopened.
        verifyBlocking(refreshPinnedMessageCache, times(2)) { invoke(any(), anyVararg()) }
    }

    @Test
    fun `unpinning a message that is not pinned is an error`() = runTest {
        whenever(pinnedMessageDao.getByTid(42L, channelId)).thenReturn(null)

        assertThat(useCase(channelId, messageTid = 42L))
            .isInstanceOf(SceytResponse.Error::class.java)
    }

    private fun eq42() = org.mockito.kotlin.eq(42L)
    private fun eq7() = org.mockito.kotlin.eq(7L)
}
