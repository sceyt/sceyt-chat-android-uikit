package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.persistence.dao.LoadRangeDao

class MessageLoadRangeUpdater(
        private val loadRangeDao: LoadRangeDao
) {

    suspend fun updateLoadRange(messageId: Long, start: Long, end: Long, channelId: Long) {
        if (start > end) return
        loadRangeDao.updateLoadRanges(start = start, end = end, messageId = messageId, channelId = channelId)
    }
}