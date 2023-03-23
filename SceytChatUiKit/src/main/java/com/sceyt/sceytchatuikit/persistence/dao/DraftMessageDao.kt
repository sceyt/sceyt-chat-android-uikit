package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageUserLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageDb

@Dao
interface DraftMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DraftMessageEntity)

    @Transaction
    @Query("select * from DraftMessageEntity where chatId = :chatId")
    suspend fun getDraftByChannelId(chatId: Long): DraftMessageDb?

/*    @Query("select * from DraftMessageEntity join users on user_id in (DraftMessageEntity.mentionUsersIds) where chatId = :chatId")
    suspend fun getDraftByChannelId2(chatId: Long): Map<DraftMessageEntity, List<UserEntity>?>*/

    @Query("delete from DraftMessageEntity where chatId = :chatId")
    suspend fun deleteDraftByChannelId(chatId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftMessageUserLinks(map: List<DraftMessageUserLink>?)
}