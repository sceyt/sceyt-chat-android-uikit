package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageEntity

@Dao
interface DraftMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DraftMessageEntity)

    @Query("select * from DraftMessageEntity where chatId = :chatId")
    suspend fun getDraftByChannelId(chatId: Long): DraftMessageEntity?

    @Query("delete from DraftMessageEntity where chatId = :chatId")
    suspend fun deleteDraftByChannelId(chatId: Long)
}