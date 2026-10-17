package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class UpdatePinnedMessagesUseCase(
    private val messageDao: MessageDao,
    private val pinnedMessageDao: PinnedMessageDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
    private val storePinsUseCase: StorePinsUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend operator fun invoke(event: PinUpdateEvent) = withContext(ioDispatcher) {
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
