package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage

/**
 * Refreshes pin state in [MessagesCache] without adding messages outside the loaded window.
 */
internal class RefreshPinnedMessageCacheUseCase(
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache,
) {

    suspend operator fun invoke(channelId: Long, vararg messageTids: Long) {
        messageTids.distinct().forEach { tid ->
            messageDao.getMessageByTid(tid)?.let {
                messagesCache.refreshMessages(channelId, it.toSceytMessage())
            }
        }
    }
}