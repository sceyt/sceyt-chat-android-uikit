package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.repositories.PinRepository

internal class SyncChannelPinsUseCase(
    private val pinnedMessageDao: PinnedMessageDao,
    private val pinRepository: PinRepository,
    private val storePinsUseCase: StorePinsUseCase,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    suspend operator fun invoke(channelId: Long) {
        val serverPinIds = mutableListOf<Long>()
        var sweptAllPages = false
        pinRepository.getPinnedMessages(channelId).collect { response ->
            when (response) {
                is SceytPagingResponse.Success -> {
                    storePinsUseCase(channelId, response.data)
                    serverPinIds += response.data.map { it.id }
                    sweptAllPages = !response.hasNext
                }

                is SceytPagingResponse.Error -> {
                    SceytLog.e(
                        TAG,
                        "syncChannelPins: fetch failed for channel $channelId," +
                                " skipping reconcile: ${response.message}"
                    )
                }
            }
        }

        if (sweptAllPages) reconcile(channelId, serverPinIds)
    }

    private suspend fun reconcile(channelId: Long, keepIds: List<Long>) {
        val staleTids = pinnedMessageDao.getExcluding(channelId, keepIds).map { it.messageTid }
        if (staleTids.isEmpty()) return
        pinnedMessageDao.deleteByTids(staleTids)
        refreshPinnedMessageCache(channelId, *staleTids.toLongArray())
    }
}
