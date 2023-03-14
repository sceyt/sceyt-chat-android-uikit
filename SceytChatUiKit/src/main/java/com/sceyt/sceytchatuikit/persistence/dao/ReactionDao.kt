package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionScoreEntity

@Dao
abstract class ReactionDao {

    @Transaction
    open suspend fun insertReactionsAndScores(messageId: Long, reactionsDb: List<ReactionEntity>, scoresDb: List<ReactionScoreEntity>) {
        deleteAllReactionScoresByMessageId(messageId)
        insertReactions(reactionsDb)
        insertReactionScores(scoresDb)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReaction(reaction: ReactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactionScores(reactionScores: List<ReactionScoreEntity>)

    @Query("select * from ReactionScoreEntity where messageId =:messageId and reaction_key =:key")
    abstract suspend fun getReactionScore(messageId: Long, key: String): ReactionScoreEntity?

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

    @Update
    abstract suspend fun updateReactionScore(reactionScore: ReactionScoreEntity)

    @Query("delete from ReactionScoreEntity where id =:id")
    abstract suspend fun deleteReactionScoreByScoreId(id: Int)

    @Query("delete from ReactionScoreEntity where messageId =:messageId")
    abstract suspend fun deleteAllReactionScoresByMessageId(messageId: Long)

    @Query("delete from ReactionEntity where messageId =:messageId and reaction_key =:key and fromId =:fromId")
    abstract suspend fun deleteReaction(messageId: Long, key: String, fromId: String)

    @Query("delete from ReactionEntity where id in (:ids)")
    abstract suspend fun deleteReactionByIds(vararg ids: Long)

    @Query("delete from ReactionEntity where messageId =:messageId")
    protected abstract suspend fun deleteAllReactionsByMessageId(messageId: Long)

    @Transaction
    open suspend fun deleteReactionAndScore(messageId: Long, key: String, fromId: String) {
        deleteReaction(messageId, key, fromId)
        getReactionScore(messageId, key)?.let {
            if (it.score > 1) {
                it.score--
                updateReactionScore(it)
            } else
                deleteReactionScoreByScoreId(it.id)
        }
    }

    @Transaction
    open suspend fun deleteAllReactionsAndScores(messageId: Long) {
        deleteAllReactionScoresByMessageId(messageId)
        deleteAllReactionsByMessageId(messageId)
    }
}
