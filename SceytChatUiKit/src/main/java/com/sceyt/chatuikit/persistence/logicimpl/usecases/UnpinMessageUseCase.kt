package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import kotlinx.coroutines.sync.withLock

/** Hides the pin immediately and retains intent until removal is acknowledged. */
internal class UnpinMessageUseCase(
    private val pinnedMessageDao: PinnedMessageDao,
    private val sendPendingPinsUseCase: SendPendingPinsUseCase,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    suspend operator fun invoke(
        channelId: Long,
        messageTid: Long,
    ): SceytResponse<Boolean> {
        val prepared = sendPendingPinsUseCase.localUpdateMutex.withLock {
            prepare(channelId, messageTid)
        }
        return when (prepared) {
            is SceytResponse.Error -> SceytResponse.Error(prepared.exception)
            is SceytResponse.Success -> prepared.data?.let { sendPendingPinsUseCase.sendUnpin(it) }
                ?: SceytResponse.Success(true)
        }
    }

    private suspend fun prepare(channelId: Long, messageTid: Long): SceytResponse<PinnedMessageEntity> {
        val existing = pinnedMessageDao.getByTid(messageTid, channelId)
        if (existing == null) {
            pinnedMessageDao.clearMessagePinMirror(messageTid)
            refreshPinnedMessageCache(channelId, messageTid)
            return createErrorResponse("Message is not pinned")
        }

        // A pending pin may already be in flight. Retain removal until the server acknowledges it.
        if (existing.syncState == PinSyncStateEntity.PendingPin.value && existing.messageId == 0L) {
            pinnedMessageDao.deleteWithMirror(messageTid, channelId)
            refreshPinnedMessageCache(channelId, messageTid)
            return SceytResponse.Success(null)
        }

        val now = maxOf(System.currentTimeMillis(), existing.lastAttemptAt + 1)
        pinnedMessageDao.markPendingUnpinWithMirror(messageTid, channelId, now)
        refreshPinnedMessageCache(channelId, messageTid)

        return SceytResponse.Success(existing.copy(
            syncState = PinSyncStateEntity.PendingUnpin.value, lastAttemptAt = now
        ))
    }
}
