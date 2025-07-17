package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.AUTO_DELETE_MESSAGES_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.AutoDeleteMessageEntity

@Dao
internal interface AutoDeleteMessageDao {

    @Query("select * from $AUTO_DELETE_MESSAGES_TABLE where channelId = :channelId and autoDeleteAt <= :localTime")
    suspend fun getOutdatedMessages(channelId: Long, localTime: Long): List<AutoDeleteMessageEntity>
}