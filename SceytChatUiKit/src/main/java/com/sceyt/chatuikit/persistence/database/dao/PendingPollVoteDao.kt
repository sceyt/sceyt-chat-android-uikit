package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_POLL_VOTE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.PendingPollVoteDb
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity

@Dao
internal abstract class PendingPollVoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: PendingPollVoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMany(entities: List<PendingPollVoteEntity>)

    @Query("DELETE FROM $PENDING_POLL_VOTE_TABLE WHERE messageTid =:messageTid and pollId = :pollId AND optionId = :optionId")
    abstract suspend fun deleteByOption(messageTid: Long, pollId: String, optionId: String): Int

    @Query("DELETE FROM $PENDING_POLL_VOTE_TABLE WHERE messageTid =:messageTid and pollId = :pollId AND optionId in(:optionIds)")
    abstract suspend fun deleteVotesByOptionIds(messageTid: Long, pollId: String, optionIds: List<String>): Int

    @Query("DELETE FROM $PENDING_POLL_VOTE_TABLE WHERE messageTid =:messageTid and pollId = :pollId")
    abstract suspend fun deletePendingVotesByPollId(messageTid: Long, pollId: String): Int

    @Transaction
    @Query("SELECT * FROM $PENDING_POLL_VOTE_TABLE ORDER BY createdAt ASC")
    abstract suspend fun getAllPendingVotesDb(): List<PendingPollVoteDb>

    @Query("SELECT * FROM $PENDING_POLL_VOTE_TABLE ORDER BY createdAt ASC")
    abstract suspend fun getAllPendingVotes(): List<PendingPollVoteEntity>
}

