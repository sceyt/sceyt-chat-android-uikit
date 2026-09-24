package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.sync.withLock

/** Sends pending intent before reconciling a complete server snapshot. */
internal class SyncChannelPinsController(
    private val pinnedMessageDao: PinnedMessageDao,
    private val pinRepository: PinRepository,
    private val sendPendingPinsUseCase: SendPendingPinsUseCase,
    private val storePinsUseCase: StorePinsUseCase,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    suspend operator fun invoke(channelId: Long) {
        sendPendingPinsUseCase(channelId)

        sendPendingPinsUseCase.requestMutex.withLock {
            val serverPinIds = mutableListOf<Long>()
            var completed = false
            pinRepository.getPinnedMessages(channelId).collect { response ->
                when (response) {
                    is SceytPagingResponse.Success -> {
                        sendPendingPinsUseCase.localUpdateMutex.withLock {
                            storePinsUseCase(channelId, response.data)
                        }
                        serverPinIds += response.data.map { it.id }

                        completed = !response.hasNext
                    }

                    is SceytPagingResponse.Error -> {
                        SceytLog.e(
                            TAG,
                            "syncChannelPins: fetch failed for channel $channelId," +
                                    " skipping reconcile: ${response.message}"
                        )
                        completed = false
                    }
                }
            }

            if (completed) sendPendingPinsUseCase.localUpdateMutex.withLock {
                reconcile(channelId, serverPinIds)
            }
        }
    }

    /** Deletes the channel's synced pins the completed sweep did not report. */
    private suspend fun reconcile(channelId: Long, keepIds: List<Long>) {
        val stale = pinnedMessageDao.getSyncedExcluding(channelId, keepIds)
        stale.forEach {
            pinnedMessageDao.deleteWithMirror(it.messageTid, it.channelId)
            refreshPinnedMessageCache(it.channelId, it.messageTid)
        }

        val drifted = pinnedMessageDao.getDriftedMirrorTids(channelId)
        if (drifted.isNotEmpty()) {
            pinnedMessageDao.repairPinMirrors(channelId)
            refreshPinnedMessageCache(channelId, *drifted.toLongArray())
        }
    }
}
