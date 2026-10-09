package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao

internal class UpdatePinnedMessagesUseCase(
    private val messageDao: MessageDao,
    private val pinnedMessageDao: PinnedMessageDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
    private val storePinsUseCase: StorePinsUseCase,
) {

    suspend operator fun invoke(event: PinUpdateEvent) {
        when (event) {
            is PinUpdateEvent.Pinned -> storePinsUseCase(event.channelId, event.messages)
            is PinUpdateEvent.Unpinned -> {
                val tids = event.messages.map { messageDao.getMessageTidById(it.messageId) ?: it.messageTid }
                pinnedMessageDao.deleteByTids(tids)
                refreshPinnedMessageCache(event.channelId, *tids.toLongArray())
            }
        }
    }
}