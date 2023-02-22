package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionScoreEntity

@Dao
abstract class ReactionDao {

    @Transaction
    open fun insertReactionsAndScores(messageId: Long, reactionsDb: List<ReactionEntity>, scoresDb: List<ReactionScoreEntity>) {
        deleteAllReactionsAndScores(messageId)
        insertReactions(reactionsDb)
        insertReactionScores(scoresDb)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertReactionScores(reactionScores: List<ReactionScoreEntity>)

    @Query("select * from ReactionScoreEntity where messageId =:messageId and reaction_key =:key")
    abstract fun getReactionScore(messageId: Long, key: String): ReactionScoreEntity?

    @Query("select * from ReactionEntity where messageId =:messageId and fromId =:myId")
    abstract suspend fun getSelfReactionsByMessageId(messageId: Long, myId: String): List<ReactionDb>

    @Update
    abstract fun updateReactionScore(reactionScore: ReactionScoreEntity)

    @Query("delete from ReactionScoreEntity where id =:id")
    abstract fun deleteReactionScoreByScoreId(id: Int)

    @Query("delete from ReactionScoreEntity where messageId =:messageId")
    abstract fun deleteAllReactionScoresByMessageId(messageId: Long)

    @Query("delete from ReactionEntity where messageId =:messageId and reaction_key =:key and fromId =:fromId")
    protected abstract fun deleteReaction(messageId: Long, key: String, fromId: String)

    @Query("delete from ReactionEntity where messageId =:messageId")
    protected abstract fun deleteAllReactionsByMessageId(messageId: Long)

    @Transaction
    open fun deleteReactionAndScore(messageId: Long, key: String, fromId: String) {
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
    open fun deleteAllReactionsAndScores(messageId: Long) {
        deleteAllReactionScoresByMessageId(messageId)
        deleteAllReactionsByMessageId(messageId)
    }
}
