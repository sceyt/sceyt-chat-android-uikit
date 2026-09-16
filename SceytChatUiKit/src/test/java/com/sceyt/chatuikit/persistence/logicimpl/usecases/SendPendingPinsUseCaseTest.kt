package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@RunWith(RobolectricTestRunner::class)
class SendPendingPinsUseCaseTest {

    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val pinRepository = mock<PinRepository>()
    private val confirmPinUseCase = mock<ConfirmPinUseCase>()
    private val refreshPinnedMessageCache = mock<RefreshPinnedMessageCacheUseCase>()
    private val useCase = SendPendingPinsUseCase(
        pinnedMessageDao,
        pinRepository,
        confirmPinUseCase,
        mock(),
        refreshPinnedMessageCache,
    )

    @org.junit.Before
    fun stubCurrentIntent() = runTest {
        org.mockito.kotlin.doSuspendableAnswer {
            pinnedMessageDao.getPendingByChannel(7L).orEmpty().firstOrNull()
        }.whenever(pinnedMessageDao) { getByTid(any(), any()) }
    }

    @Test
    fun `a pending pin without a server message id waits for its message acknowledgement`() = runTest {
        whenever(pinnedMessageDao.getPendingByChannel(7L)).thenReturn(
            listOf(
                pinnedEntity(
                    messageId = 0L,
                    syncState = PinSyncState.PendingPin.value,
                )
            )
        )

        useCase(channelId = 7L)

        verifyBlocking(pinRepository, never()) { pinMessages(any(), any(), any(), anyOrNull()) }
    }

    @Test
    fun `a pending pin preserves its scope and expiry when it is retried`() = runTest {
        whenever(pinnedMessageDao.getPendingByChannel(7L)).thenReturn(
            listOf(
                pinnedEntity(
                    pinScope = 1,
                    pinnedUntil = 9_000L,
                    syncState = PinSyncState.PendingPin.value,
                )
            )
        )
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Success(emptyList()))

        useCase(channelId = 7L)

        verifyBlocking(pinRepository) {
            pinMessages(eq(7L), eq(listOf(42L)), eq(PinType.PERSONAL), eq(9_000L))
        }
    }

    @Test
    fun `a failed pending pin remains queued and records another attempt`() = runTest {
        whenever(pinnedMessageDao.getPendingByChannel(7L)).thenReturn(
            listOf(pinnedEntity(syncState = PinSyncState.PendingPin.value))
        )
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Error(null))

        useCase(channelId = 7L)

        verifyBlocking(pinnedMessageDao) { incrementRetry(eq(42L), eq(7L), any()) }
        verify(pinnedMessageDao, never()).deleteWithMirror(any(), any())
    }

    @Test
    fun `a successful pending pin is confirmed with the server pin id`() = runTest {
        whenever(confirmPinUseCase.invoke(any(), any(), any()))
            .thenReturn(ConfirmPinResponse(null, didFlipPendingIntent = false))
        val serverPin = sceytPinnedMessage(id = 900L)
        whenever(pinnedMessageDao.getPendingByChannel(7L)).thenReturn(
            listOf(pinnedEntity(syncState = PinSyncState.PendingPin.value))
        )
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenReturn(SceytResponse.Success(listOf(serverPin)))

        useCase(channelId = 7L)

        verifyBlocking(confirmPinUseCase) { invoke(eq(7L), eq(42L), eq(900L)) }
    }

    @Test
    fun `a successful pending unpin deletes its durable intent`() = runTest {
        whenever(pinnedMessageDao.getPendingByChannel(7L)).thenReturn(
            listOf(
                pinnedEntity(
                    syncState = PinSyncState.PendingUnpin.value,
                    messageId = 42L,
                )
            )
        )
        whenever(pinRepository.unpinMessages(any(), any()))
            .thenReturn(SceytResponse.Success(emptyList()))

        useCase(channelId = 7L)

        verifyBlocking(pinRepository) { unpinMessages(eq(7L), eq(listOf(42L))) }
        verify(pinnedMessageDao).deleteWithMirror(42L, 7L)
    }

    @Test
    fun `a failed pending unpin remains queued and records another attempt`() = runTest {
        whenever(pinnedMessageDao.getPendingByChannel(7L)).thenReturn(
            listOf(pinnedEntity(syncState = PinSyncState.PendingUnpin.value))
        )
        whenever(pinRepository.unpinMessages(any(), any()))
            .thenReturn(SceytResponse.Error(null))

        useCase(channelId = 7L)

        verifyBlocking(pinnedMessageDao) { incrementRetry(eq(42L), eq(7L), any()) }
        verify(pinnedMessageDao, never()).deleteWithMirror(any(), any())
    }

    @Test
    fun `an unscoped resend drains pending intents from every channel`() = runTest {
        whenever(pinnedMessageDao.getAllPending()).thenReturn(emptyList())

        useCase()

        verifyBlocking(pinnedMessageDao) { getAllPending() }
        verifyBlocking(pinnedMessageDao, never()) { getPendingByChannel(any()) }
    }

    @Test
    fun `a permanently rejected pin is removed instead of retried`() = runTest {
        val error = mock<SceytException> { on { type }.thenReturn("NotAllowed") }
        whenever(pinRepository.pinMessages(any(), any(), any(), anyOrNull()))
            .thenAnswer { SceytResponse.Error<List<SceytPinnedMessage>>(error) }

        org.mockito.kotlin.doReturn(pinnedEntity(syncState = PinSyncState.PendingPin.value))
            .whenever(pinnedMessageDao).getByTid(42L, 7L)
        val response = useCase.sendPin(pinnedEntity(syncState = PinSyncState.PendingPin.value))

        assertThat((response as SceytResponse.Error).exception).isSameInstanceAs(error)
        verify(pinnedMessageDao).deleteWithMirror(42L, 7L)
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 42L) }
        verify(pinnedMessageDao, never()).incrementRetry(any(), any(), any())
    }

    @Test
    fun `a permanently rejected unpin restores the confirmed pin`() = runTest {
        val error = mock<SceytException> { on { type }.thenReturn("NotAllowed") }
        whenever(pinRepository.unpinMessages(any(), any())).thenAnswer { SceytResponse.Error<List<SceytPinnedMessage>>(error) }

        org.mockito.kotlin.doReturn(pinnedEntity(syncState = PinSyncState.PendingUnpin.value))
            .whenever(pinnedMessageDao).getByTid(42L, 7L)
        val response = useCase.sendUnpin(pinnedEntity(syncState = PinSyncState.PendingUnpin.value))

        assertThat((response as SceytResponse.Error).exception).isSameInstanceAs(error)
        val restored = argumentCaptor<PinnedMessageEntity>()
        verify(pinnedMessageDao).upsertWithMirror(restored.capture())
        assertThat(restored.firstValue.syncState).isEqualTo(PinSyncState.Synced.value)
        verifyBlocking(refreshPinnedMessageCache) { invoke(7L, 42L) }
        verify(pinnedMessageDao, never()).incrementRetry(any(), any(), any())
    }

    @Test
    fun `unpinning a server message that no longer exists removes local intent`() = runTest {
        val error = mock<SceytException> { on { type }.thenReturn("NotFound") }
        whenever(pinRepository.unpinMessages(any(), any())).thenAnswer { SceytResponse.Error<List<SceytPinnedMessage>>(error) }

        org.mockito.kotlin.doReturn(pinnedEntity(syncState = PinSyncState.PendingUnpin.value))
            .whenever(pinnedMessageDao).getByTid(42L, 7L)
        useCase.sendUnpin(pinnedEntity(syncState = PinSyncState.PendingUnpin.value))

        verify(pinnedMessageDao).deleteWithMirror(42L, 7L)
        verify(pinnedMessageDao, never()).upsertWithMirror(any())
    }

}
