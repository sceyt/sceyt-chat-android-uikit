package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
class SyncChannelPinsUseCaseTest {

    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val pinRepository = mock<PinRepository>()
    private val sendPendingPinsUseCase = mock<SendPendingPinsUseCase>()
    private val storePinsUseCase = mock<StorePinsUseCase>()

    private val useCase = SyncChannelPinsUseCase(
        pinnedMessageDao = pinnedMessageDao,
        pinRepository = pinRepository,
        sendPendingPinsUseCase = sendPendingPinsUseCase,
        storePinsUseCase = storePinsUseCase,
        refreshPinnedMessageCache = mock(),
    )

    private val channelId = 7L

    @Test
    fun `concurrent channel sweeps reconcile only their own pages`() = runTest {
        val firstPageStored = CompletableDeferred<Unit>()
        val otherChannelCompleted = CompletableDeferred<Unit>()
        val firstPin = sceytPinnedMessage(id = 10L)
        val secondPin = sceytPinnedMessage(id = 11L)
        val otherPin = sceytPinnedMessage(id = 20L)
        whenever(pinnedMessageDao.getSyncedExcluding(any(), any())).thenReturn(emptyList())
        whenever(pinRepository.getPinnedMessages(7L)).thenReturn(flow {
            emit(SceytPagingResponse.Success(listOf(firstPin), hasNext = true))
            firstPageStored.complete(Unit)
            otherChannelCompleted.await()
            emit(SceytPagingResponse.Success(listOf(secondPin), hasNext = false))
        })
        whenever(pinRepository.getPinnedMessages(8L)).thenReturn(
            flowOf(SceytPagingResponse.Success(listOf(otherPin), hasNext = false))
        )

        val firstSync = launch { useCase(7L) }
        firstPageStored.await()
        val secondSync = launch { useCase(8L) }
        otherChannelCompleted.complete(Unit)
        firstSync.join()
        secondSync.join()

        verify(pinnedMessageDao).getSyncedExcluding(7L, listOf(10L, 11L))
        verify(pinnedMessageDao).getSyncedExcluding(8L, listOf(20L))
        verify(storePinsUseCase).invoke(7L, listOf(secondPin))
    }

    @Before
    fun stubMirrorRepair() = runTest {
        whenever(sendPendingPinsUseCase.localUpdateMutex).thenReturn(kotlinx.coroutines.sync.Mutex())
        whenever(sendPendingPinsUseCase.requestMutex).thenReturn(kotlinx.coroutines.sync.Mutex())
        whenever(pinnedMessageDao.getDriftedMirrorTids(any())).thenReturn(emptyList())
    }

    @Test
    fun `a failed fetch never deletes local pins`() = runTest {
        whenever(pinRepository.getPinnedMessages(channelId))
            .thenReturn(flowOf(SceytPagingResponse.Error(null)))

        useCase(channelId)

        // A half-read answer must not drive a reconcile.
        verifyBlocking(pinnedMessageDao, never()) { getSyncedExcluding(any(), any()) }
        verify(pinnedMessageDao, never()).deleteWithMirror(any(), any())
    }

    @Test
    fun `a page failure part-way through still skips the reconcile`() = runTest {
        whenever(pinRepository.getPinnedMessages(channelId))
            .thenReturn(flowOf(
                SceytPagingResponse.Success(emptyList(), hasNext = true),
                SceytPagingResponse.Error(null)
            ))

        useCase(channelId)

        verifyBlocking(pinnedMessageDao, never()) { getSyncedExcluding(any(), any()) }
    }

    @Test
    fun `pending intents are sent before the server is read`() = runTest {
        whenever(pinRepository.getPinnedMessages(channelId))
            .thenReturn(flowOf(SceytPagingResponse.Success(emptyList(), hasNext = false)))
        whenever(pinnedMessageDao.getSyncedExcluding(any(), any())).thenReturn(emptyList())

        useCase(channelId)

        val order = org.mockito.kotlin.inOrder(sendPendingPinsUseCase, pinRepository)
        order.verify(sendPendingPinsUseCase).invoke(channelId)
        order.verify(pinRepository).getPinnedMessages(channelId)
    }

    @Test
    fun `walks every page until hasNext is false, storing each as it lands`() = runTest {
        whenever(pinRepository.getPinnedMessages(channelId))
            .thenReturn(flowOf(
                SceytPagingResponse.Success(emptyList(), hasNext = true),
                SceytPagingResponse.Success(emptyList(), hasNext = true),
                SceytPagingResponse.Success(emptyList(), hasNext = false)
            ))
        whenever(pinnedMessageDao.getSyncedExcluding(any(), any())).thenReturn(emptyList())

        useCase(channelId)

        // Three pages stored, not one batch at the end, so the banner grows monotonically.
        verify(storePinsUseCase, times(3)).invoke(eq(channelId), any<List<SceytPinnedMessage>>())
    }

    @Test
    fun `a completed sweep deletes only the synced pins the server did not report`() = runTest {
        whenever(pinRepository.getPinnedMessages(channelId))
            .thenReturn(flowOf(SceytPagingResponse.Success(emptyList(), hasNext = false)))
        whenever(pinnedMessageDao.getSyncedExcluding(any(), any())).thenReturn(
            listOf(pinnedEntity(messageTid = 5L, syncState = PinSyncState.Synced.value))
        )

        useCase(channelId)

        verify(pinnedMessageDao).deleteWithMirror(5L, channelId)
    }
}
