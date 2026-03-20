package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.REACTION_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.REACTION_TOTAL_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionDb
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionTotalEntity

@Dao
internal abstract class ReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReaction(reaction: ReactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReactionTotals(reactionTotals: List<ReactionTotalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReactionTotal(reactionTotal: ReactionTotalEntity)

    @Transaction
    open suspend fun insertReactionsIfMessageExist(reactions: List<ReactionEntity>) {
        reactions.groupBy { it.messageId }.forEach { (messageId, reactions) ->
            if (checkExistMessage(messageId) == messageId) {
                insertReactions(reactions)
            }
        }
    }

    @Transaction
    open suspend fun insertMessageReactionsAndTotalsIfMessageExist(
        messageId: Long,
        reactions: List<ReactionEntity>?,
        reactionTotals: List<ReactionTotalEntity>?
    ) {
        if (reactions.isNullOrEmpty() && reactionTotals.isNullOrEmpty())
            return

        if (checkExistMessage(messageId) == messageId) {
            reactions?.let { insertReactions(it) }
            reactionTotals?.let { insertReactionTotals(it) }
        }
    }

    @Transaction
    open suspend fun insertReactionAndIncreaseTotalIfNeeded(entity: ReactionEntity) {
        if (checkExistMessage(entity.messageId) != entity.messageId)
            return

        // Check maybe reaction already added by me, and ignore adding reactionTotal.
        val reaction = entity.fromId?.let { userId ->
            getUserReactionByKey(entity.messageId, userId, entity.key)
        }
        insertReaction(entity)

        if (reaction == null)
            increaseReactionTotal(entity.messageId, entity.key, entity.score)
    }

    @Query("SELECT message_id FROM $MESSAGE_TABLE WHERE message_id = :messageId")
    protected abstract suspend fun checkExistMessage(messageId: Long): Long?

    @Query("SELECT * FROM $REACTION_TOTAL_TABLE WHERE messageId = :messageId AND reaction_key = :key")
    abstract suspend fun getReactionTotal(messageId: Long, key: String): ReactionTotalEntity?

    @Transaction
    @Query("SELECT * FROM $REACTION_TABLE WHERE id = :id")
    abstract suspend fun getReactionsById(id: Long): ReactionDb?

    @Transaction
    @Query("SELECT * FROM $REACTION_TABLE WHERE messageId = :messageId")
    abstract suspend fun getReactionsByMsgId(messageId: Long): List<ReactionDb>

    @Transaction
    @Query("SELECT * FROM $REACTION_TABLE WHERE messageId = :messageId AND reaction_key = :key")
    abstract suspend fun getReactionsByMsgIdAndKey(messageId: Long, key: String): List<ReactionDb>

    @Transaction
    @Query("SELECT * FROM $REACTION_TABLE WHERE messageId = :messageId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getReactions(messageId: Long, limit: Int, offset: Int): List<ReactionDb>

    @Transaction
    @Query(
        """
        SELECT *
        FROM $REACTION_TABLE
        WHERE messageId = :messageId
          AND reaction_key = :key
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getReactionsByKey(messageId: Long, limit: Int, offset: Int, key: String): List<ReactionDb>

    @Transaction
    @Query("SELECT * FROM $REACTION_TABLE WHERE messageId = :messageId AND fromId = :myId")
    abstract suspend fun getSelfReactionsByMessageId(messageId: Long, myId: String): List<ReactionDb>

    @Transaction
    @Query("SELECT * FROM $REACTION_TABLE WHERE messageId = :messageId AND fromId = :userId AND reaction_key = :key")
    abstract suspend fun getUserReactionByKey(messageId: Long, userId: String, key: String): ReactionDb?

    @Update
    protected abstract suspend fun updateReactionTotal(reactionTotal: ReactionTotalEntity)

    @Query("DELETE FROM $REACTION_TOTAL_TABLE WHERE id = :id")
    abstract suspend fun deleteReactionTotalByTotalId(id: Int)

    @Query("DELETE FROM $REACTION_TOTAL_TABLE WHERE messageId = :messageId")
    abstract suspend fun deleteAllReactionTotalsByMessageId(messageId: Long)

    @Query("DELETE FROM $REACTION_TABLE WHERE messageId = :messageId AND reaction_key = :key AND fromId = :fromId")
    abstract suspend fun deleteReaction(messageId: Long, key: String, fromId: String?): Int

    @Query("DELETE FROM $REACTION_TABLE WHERE id IN (:ids)")
    abstract suspend fun deleteReactionByIds(vararg ids: Long)

    @Query("DELETE FROM $REACTION_TABLE WHERE messageId = :messageId")
    protected abstract suspend fun deleteAllReactionsByMessageId(messageId: Long)

    @Transaction
    open suspend fun deleteReactionAndTotal(messageId: Long, key: String, fromId: String?, score: Int) {
        val row = deleteReaction(messageId, key, fromId)
        if (row > 0)
            getReactionTotal(messageId, key)?.let {
                if (it.score - score >= 1) {
                    val newTotal = it.copy(score = it.score - score)
                    updateReactionTotal(newTotal)
                } else
                    deleteReactionTotalByTotalId(it.id)
            }
    }

    private suspend fun increaseReactionTotal(messageId: Long, key: String, score: Int) {
        getReactionTotal(messageId, key)?.let {
            val newTotal = it.copy(score = it.score + score)
            insertReactionTotal(newTotal)
        } ?: run {
            insertReactionTotal(ReactionTotalEntity(messageId = messageId,
                key = key, score = score, count = 1))
        }
    }

    @Transaction
    open suspend fun deleteAllReactionsAndTotals(messageId: Long) {
        deleteAllReactionTotalsByMessageId(messageId)
        deleteAllReactionsByMessageId(messageId)
    }
}
