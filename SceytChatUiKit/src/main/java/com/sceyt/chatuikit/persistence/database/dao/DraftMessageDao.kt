package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageUserLink

@Dao
abstract class DraftMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: DraftMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDraftMessageUserLinks(links: List<DraftMessageUserLink>)

    @Transaction
    open suspend fun insertWithUserLinks(entity: DraftMessageEntity, links: List<DraftMessageUserLink>) {
        if (checkExistChannel(entity.chatId) > 0) {
            insert(entity)
            links.takeIf { it.isNotEmpty() }?.let {
                insertDraftMessageUserLinks(it)
            }
        }
    }

    @Query("select count(*) from channels where chat_id = :chatId")
    abstract suspend fun checkExistChannel(chatId: Long): Int

    @Transaction
    @Query("select * from DraftMessageEntity where chatId = :chatId")
    abstract suspend fun getDraftByChannelId(chatId: Long): DraftMessageDb?

    @Query("delete from DraftMessageEntity where chatId = :chatId")
    abstract suspend fun deleteDraftByChannelId(chatId: Long)
}