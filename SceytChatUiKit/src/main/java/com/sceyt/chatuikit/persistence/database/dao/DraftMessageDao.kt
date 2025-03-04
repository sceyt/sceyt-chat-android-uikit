package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.entity.channel.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.DRAFT_MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageUserLinkEntity

@Dao
internal abstract class DraftMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: DraftMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDraftMessageUserLinks(links: List<DraftMessageUserLinkEntity>)

    @Transaction
    open suspend fun insertWithUserLinks(entity: DraftMessageEntity, links: List<DraftMessageUserLinkEntity>) {
        if (checkExistChannel(entity.chatId) > 0) {
            insert(entity)
            links.takeIf { it.isNotEmpty() }?.let {
                insertDraftMessageUserLinks(it)
            }
        }
    }

    @Query("select count(*) from $CHANNEL_TABLE where chat_id = :chatId")
    abstract suspend fun checkExistChannel(chatId: Long): Int

    @Transaction
    @Query("select * from $DRAFT_MESSAGE_TABLE where chatId = :chatId")
    abstract suspend fun getDraftByChannelId(chatId: Long): DraftMessageDb?

    @Query("delete from $DRAFT_MESSAGE_TABLE where chatId = :chatId")
    abstract suspend fun deleteDraftByChannelId(chatId: Long)
}