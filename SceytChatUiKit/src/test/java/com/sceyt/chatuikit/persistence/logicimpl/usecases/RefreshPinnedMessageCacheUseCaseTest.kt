package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RefreshPinnedMessageCacheUseCaseTest {
    private val messageDao = mock<MessageDao>()
    private val cache = MessagesCache()
    private val useCase = RefreshPinnedMessageCacheUseCase(messageDao, cache)

    @Test
    fun `refreshing an old pin does not insert it into the loaded timeline`() = runTest {
        cache.add(7L, sceytMessage(id = 100L, tid = 100L))
        whenever(messageDao.getMessageByTid(42L)).thenReturn(
            messageDb(messageEntity().copy(unList = true))
        )

        useCase(7L, 42L)

        assertThat(cache.getSorted(7L).map { it.tid }).containsExactly(100L)
    }

    @Test
    fun `refresh updates pin details on an already loaded message`() = runTest {
        cache.add(7L, sceytMessage())
        whenever(messageDao.getMessageByTid(42L)).thenReturn(
            messageDb().copy(pinnedMessage = pinnedEntity(syncState = PinSyncStateEntity.Synced.value))
        )

        useCase(7L, 42L)

        assertThat(cache.get(7L, 42L)?.pinDetails?.isPinned).isTrue()
        assertThat(cache.getSorted(7L)).hasSize(1)
    }

    @Test
    fun `refresh does not recreate an empty channel cache`() = runTest {
        whenever(messageDao.getMessageByTid(42L)).thenReturn(messageDb())

        useCase(7L, 42L)

        assertThat(cache.getSorted(7L)).isEmpty()
    }
}
