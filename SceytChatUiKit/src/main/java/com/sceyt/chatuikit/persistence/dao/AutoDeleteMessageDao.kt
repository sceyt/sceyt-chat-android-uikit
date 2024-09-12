package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.entity.messages.AutoDeleteMessageEntity

@Dao
interface AutoDeleteMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoDeletedMessages(messages: List<AutoDeleteMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoDeletedMessage(message: AutoDeleteMessageEntity)

    @Query("select * from AutoDeleteMessages where channelId = :channelId and autoDeleteAt <= :localTime")
    suspend fun getOutdatedMessages(channelId: Long, localTime: Long): List<AutoDeleteMessageEntity>

}