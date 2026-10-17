package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_REACTION_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingReactionEntity

@Dao
internal abstract class PendingReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(entity: PendingReactionEntity): Long

    open suspend fun insertIfMessageExist(entity: PendingReactionEntity) {
        if (checkExistMessage(entity.messageId) == entity.messageId)
            insert(entity)
    }

    @Query("SELECT message_id FROM $MESSAGE_TABLE WHERE message_id = :messageId")
    protected abstract suspend fun checkExistMessage(messageId: Long): Long?

    @Query("SELECT * FROM $PENDING_REACTION_TABLE")
    abstract suspend fun getAll(): List<PendingReactionEntity>

    @Query("SELECT * FROM $PENDING_REACTION_TABLE WHERE channelId = :channelId")
    abstract suspend fun getAllByChannelId(channelId: Long): List<PendingReactionEntity>

    @Query("SELECT * FROM $PENDING_REACTION_TABLE WHERE messageId = :messageId")
    abstract suspend fun getAllByMsgId(messageId: Long): List<PendingReactionEntity>

    @Query("SELECT * FROM $PENDING_REACTION_TABLE WHERE messageId = :messageId AND reaction_key = :key")
    abstract suspend fun getAllByMsgIdAndKey(messageId: Long, key: String): List<PendingReactionEntity>

    @Query("DELETE FROM $PENDING_REACTION_TABLE WHERE messageId = :messageId AND reaction_key = :key")
    abstract suspend fun deletePendingReaction(messageId: Long, key: String)

    @Query("UPDATE $PENDING_REACTION_TABLE SET channelId = :newChannelId WHERE channelId = :oldChannelId")
    abstract suspend fun updateChannelId(oldChannelId: Long, newChannelId: Long): Int
}
