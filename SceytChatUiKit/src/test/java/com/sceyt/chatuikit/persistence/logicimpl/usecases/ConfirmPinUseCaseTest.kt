package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ConfirmPinUseCaseTest {

    private val pinnedMessageDao = mock<PinnedMessageDao>()
    private val useCase = ConfirmPinUseCase(pinnedMessageDao)

    @Test
    fun `only a successful pending transition announces a pin`() = runTest {
        whenever(pinnedMessageDao.markSynced(42L, 7L, 900L)).thenReturn(1, 0)
        whenever(pinnedMessageDao.getPinnedMessages(any(), any())).thenReturn(emptyList())

        val first = useCase(7L, 42L, 900L)
        val duplicate = useCase(7L, 42L, 900L)

        assertThat(first.didFlipPendingIntent).isTrue()
        assertThat(duplicate.didFlipPendingIntent).isFalse()
    }

    @Test
    fun `an acknowledgement that does not match pending intent cannot announce a pin`() = runTest {
        whenever(pinnedMessageDao.markSynced(42L, 7L, 900L)).thenReturn(0)
        whenever(pinnedMessageDao.getPinnedMessages(any(), any())).thenReturn(emptyList())

        val response = useCase(7L, 42L, 900L)

        assertThat(response.pinnedMessage).isNull()
        assertThat(response.didFlipPendingIntent).isFalse()
    }
}
