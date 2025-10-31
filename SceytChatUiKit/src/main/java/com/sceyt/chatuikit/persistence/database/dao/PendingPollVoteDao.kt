package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_POLL_VOTE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity

@Dao
internal abstract class PendingPollVoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(pendingVote: PendingPollVoteEntity)

    @Query("DELETE FROM $PENDING_POLL_VOTE_TABLE WHERE messageTid =:messageTid and pollId = :pollId AND optionId = :optionId")
    abstract suspend fun deleteByOption(messageTid: Long, pollId: String, optionId: String): Int

    @Query("DELETE FROM $PENDING_POLL_VOTE_TABLE WHERE messageTid =:messageTid and pollId = :pollId")
    abstract suspend fun deletePendingVotesByPollId(messageTid: Long, pollId: String): Int

    @Transaction
    open suspend fun insertIfMessageExists(pendingVote: PendingPollVoteEntity) {
        insert(pendingVote)
    }

    // Get all pending votes for sync
    @Query("SELECT * FROM $PENDING_POLL_VOTE_TABLE ORDER BY createdAt ASC")
    abstract suspend fun getAllPendingVotes(): List<PendingPollVoteEntity>
}

