package com.sceyt.chatuikit.persistence.logicimpl.message

import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao

class MessageLoadRangeUpdater(
        private val loadRangeDao: LoadRangeDao
) {

    suspend fun updateLoadRange(messageId: Long, start: Long, end: Long, channelId: Long) {
        if (start > end) return
        loadRangeDao.updateLoadRanges(start = start, end = end, messageId = messageId, channelId = channelId)
    }
}