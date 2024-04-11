package com.sceyt.chatuikit.persistence.dao

import androidx.room.*
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageUserLink

@Dao
interface DraftMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DraftMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftMessageUserLinks(links: List<DraftMessageUserLink>)

    @Transaction
    suspend fun insertWithUserLinks(entity: DraftMessageEntity, links: List<DraftMessageUserLink>?) {
        insert(entity)
        links?.let { insertDraftMessageUserLinks(it) }
    }

    @Transaction
    @Query("select * from DraftMessageEntity where chatId = :chatId")
    suspend fun getDraftByChannelId(chatId: Long): DraftMessageDb?

    @Query("delete from DraftMessageEntity where chatId = :chatId")
    suspend fun deleteDraftByChannelId(chatId: Long)
}