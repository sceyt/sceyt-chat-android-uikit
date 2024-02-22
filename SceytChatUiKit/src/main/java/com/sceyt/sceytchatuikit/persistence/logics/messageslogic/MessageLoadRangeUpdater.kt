package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.persistence.dao.LoadRangeDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.LoadRangeEntity

class MessageLoadRangeUpdater(
        private val loadRangeDao: LoadRangeDao
) {

    suspend fun updateMessageLoadRange(messageId: Long, start: Long, end: Long, channelId: Long) {
        loadRangeDao.updateLoadRanges(start = start, end = end, messageId = messageId, channelId = channelId)
    }

    suspend fun getMessageLoadRange(messageId: Long): List<LoadRangeEntity> {
        return loadRangeDao.getLoadRange(messageId)
    }
}