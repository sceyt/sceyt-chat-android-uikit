package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.*
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionDb
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionTotalEntity

@Dao
abstract class ReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReaction(reaction: ReactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactionTotals(reactionTotals: List<ReactionTotalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactionTotal(reactionTotal: ReactionTotalEntity)

    @Query("select * from ReactionTotalEntity where messageId =:messageId and reaction_key =:key")
    abstract suspend fun getReactionTotal(messageId: Long, key: String): ReactionTotalEntity?

    @Transaction
    @Query("select * from ReactionEntity where messageId =:messageId")
    abstract suspend fun getReactionsByMsgId(messageId: Long): List<ReactionDb>

    @Transaction
    @Query("select * from ReactionEntity where messageId =:messageId and reaction_key =:key")
    abstract suspend fun getReactionsByMsgIdAndKey(messageId: Long, key: String): List<ReactionDb>

    @Transaction
    @Query("select * from ReactionEntity where messageId =:messageId order by id desc limit :limit offset :offset")
    abstract suspend fun getReactions(messageId: Long, limit: Int, offset: Int): List<ReactionDb>

    @Transaction
    @Query("select * from ReactionEntity where messageId =:messageId and reaction_key =:key " +
            "order by id desc limit :limit offset :offset")
    abstract suspend fun getReactionsByKey(messageId: Long, limit: Int, offset: Int, key: String): List<ReactionDb>

    @Transaction
    @Query("select * from ReactionEntity where messageId =:messageId and fromId =:myId ")
    abstract suspend fun getSelfReactionsByMessageId(messageId: Long, myId: String): List<ReactionDb>

    @Transaction
    @Query("select * from ReactionEntity where messageId =:messageId and fromId =:userId and reaction_key =:key")
    abstract suspend fun getUserReactionByKey(messageId: Long, userId: String, key: String): ReactionDb?

    @Update
    abstract suspend fun updateReactionTotal(reactionTotal: ReactionTotalEntity)

    @Query("delete from ReactionTotalEntity where id =:id")
    abstract suspend fun deleteReactionTotalByTotalId(id: Int)

    @Query("delete from ReactionTotalEntity where messageId =:messageId")
    abstract suspend fun deleteAllReactionTotalsByMessageId(messageId: Long)

    @Query("delete from ReactionEntity where messageId =:messageId and reaction_key =:key and fromId =:fromId")
    abstract suspend fun deleteReaction(messageId: Long, key: String, fromId: String?): Int

    @Query("delete from ReactionEntity where id in (:ids)")
    abstract suspend fun deleteReactionByIds(vararg ids: Long)

    @Query("delete from ReactionEntity where messageId =:messageId")
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

    @Transaction
    open suspend fun deleteAllReactionsAndTotals(messageId: Long) {
        deleteAllReactionTotalsByMessageId(messageId)
        deleteAllReactionsByMessageId(messageId)
    }
}
