package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.entity.messages.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PENDING_REACTION_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingReactionEntity

@Dao
abstract class PendingReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(entity: PendingReactionEntity): Long

    open suspend fun insertIfMessageExist(entity: PendingReactionEntity) {
        if (checkExistMessage(entity.messageId) == entity.messageId)
            insert(entity)
    }

    @Query("select message_id from $MESSAGE_TABLE where message_id = :messageId")
    protected abstract suspend fun checkExistMessage(messageId: Long): Long?

    @Query("select * from $PENDING_REACTION_TABLE")
    abstract suspend fun getAll(): List<PendingReactionEntity>

    @Query("select * from $PENDING_REACTION_TABLE where channelId =:channelId")
    abstract suspend fun getAllByChannelId(channelId: Long): List<PendingReactionEntity>

    @Query("select * from $PENDING_REACTION_TABLE where messageId =:messageId")
    abstract suspend fun getAllByMsgId(messageId: Long): List<PendingReactionEntity>

    @Query("select * from $PENDING_REACTION_TABLE where messageId =:messageId and reaction_key =:key")
    abstract suspend fun getAllByMsgIdAndKey(messageId: Long, key: String): List<PendingReactionEntity>

    @Query("delete from $PENDING_REACTION_TABLE where messageId =:messageId and reaction_key =:key")
    abstract suspend fun deletePendingReaction(messageId: Long, key: String)
}