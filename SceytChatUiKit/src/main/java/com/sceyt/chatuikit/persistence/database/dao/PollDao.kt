package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.POLL_OPTION_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.POLL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.POLL_VOTE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.PollDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PollEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PollOptionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PollVoteEntity

@Dao
internal abstract class PollDao {

    @Transaction
    @Query("SELECT * FROM $POLL_TABLE WHERE id = :pollId")
    abstract suspend fun getPollById(pollId: String): PollDb?

    @Transaction
    open suspend fun upsertPoll(pollDb: PollDb) {
        // Check if message exists
        if (checkExistMessage(pollDb.pollEntity.messageTid) != pollDb.pollEntity.messageTid)
            return

        // Insert/update poll entity
        insertPollReplace(pollDb.pollEntity)

        // Delete old options (cascade will handle votes)
        deletePollOptions(pollDb.pollEntity.id)

        // Insert new options
        if (pollDb.options.isNotEmpty()) {
            insertPollOptions(pollDb.options)
        }

        // Delete all old votes and insert new ones
        deleteAllVotesByPollId(pollDb.pollEntity.id)
        val votes = pollDb.votes?.map { it.vote } ?: emptyList()
        if (votes.isNotEmpty()) {
            insertVotesReplace(votes)
        }
    }

    @Query("DELETE FROM $POLL_VOTE_TABLE WHERE pollId = :pollId AND optionId = :optionId AND userId = :userId")
    abstract suspend fun deleteVote(pollId: String, optionId: String, userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertVotesReplace(votes: List<PollVoteEntity>)

    @Query("DELETE FROM $POLL_VOTE_TABLE WHERE pollId = :pollId")
    protected abstract suspend fun deleteAllVotesByPollId(pollId: String)

    // Protected methods for internal use
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPollReplace(poll: PollEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPollOptions(options: List<PollOptionEntity>)

    @Query("DELETE FROM $POLL_OPTION_TABLE WHERE pollId = :pollId")
    protected abstract suspend fun deletePollOptions(pollId: String)

    @Query("SELECT tid FROM $MESSAGE_TABLE WHERE tid = :messageTid")
    protected abstract suspend fun checkExistMessage(messageTid: Long): Long?
}
