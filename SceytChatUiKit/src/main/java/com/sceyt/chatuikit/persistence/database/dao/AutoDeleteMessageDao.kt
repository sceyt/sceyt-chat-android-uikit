package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.entity.messages.AutoDeleteMessageEntity

@Dao
interface AutoDeleteMessageDao {

    @Query("select * from AutoDeleteMessages where channelId = :channelId and autoDeleteAt <= :localTime")
    suspend fun getOutdatedMessages(channelId: Long, localTime: Long): List<AutoDeleteMessageEntity>
}