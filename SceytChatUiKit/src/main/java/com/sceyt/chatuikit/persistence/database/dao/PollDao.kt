package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
    @Query("SELECT * FROM $POLL_TABLE WHERE messageTid =:messageTid AND id = :pollId")
    abstract suspend fun getPollById(messageTid: Long, pollId: String): PollDb?

    @Query("SELECT * FROM $POLL_TABLE WHERE messageTid =:messageTid AND id = :pollId")
    abstract suspend fun getPollEntityById(messageTid: Long, pollId: String): PollEntity?

    @Transaction
    open suspend fun upsertPollEntityWithVotes(
        entity: PollEntity,
        votes: List<PollVoteEntity>
    ) {
        if (!existsMessageByTid(entity.messageTid))
            return

        upsertPollEntity(entity)

        // Insert votes
        if (votes.isNotEmpty())
            insertVotesReplace(votes)
    }

    @Transaction
    open suspend fun upsertPollEntityWithVotes(
        entity: PollEntity,
        addedVotes: List<PollVoteEntity>,
        deletedVotes: List<PollVoteEntity>,
    ) {
        if (!existsMessageByTid(entity.messageTid))
            return

        upsertPollEntity(entity)

        // Delete votes
        deletedVotes.groupBy { it.userId }.forEach { entry ->
            val (userId, votes) = entry
            deleteUserVotes(entity.id, userId, votes.map { it.optionId })
        }

        // Insert votes
        if (addedVotes.isNotEmpty())
            insertVotesReplace(addedVotes)
    }

    protected suspend fun upsertPollEntity(entity: PollEntity) {
        val rowId = insertPollIgnored(entity)
        if (rowId == -1L) {
            updatePollIgnored(entity) == 1
        }
    }

    @Query("DELETE FROM $POLL_VOTE_TABLE WHERE pollId = :pollId AND optionId = :optionId AND userId = :userId")
    abstract suspend fun deleteUserVote(pollId: String, optionId: String, userId: String): Int

    @Query("DELETE FROM $POLL_VOTE_TABLE WHERE pollId = :pollId AND userId = :userId AND optionId IN (:optionIds)")
    abstract suspend fun deleteUserVotes(pollId: String, userId: String, optionIds: List<String>)

    @Query("DELETE FROM $POLL_VOTE_TABLE WHERE pollId = :pollId AND userId = :userId")
    abstract suspend fun deleteUserAllVotes(pollId: String, userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVotesReplace(votes: List<PollVoteEntity>)

    @Query("DELETE FROM $POLL_VOTE_TABLE WHERE pollId = :pollId")
    protected abstract suspend fun deleteAllVotesByPollId(pollId: String)

    @Query("DELETE FROM $POLL_VOTE_TABLE WHERE pollId = :pollId AND userId != :excludedUserId")
    protected abstract suspend fun deleteOthersVotesByPollId(pollId: String, excludedUserId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPollReplace(poll: PollEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPollIgnored(poll: PollEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPollOptions(options: List<PollOptionEntity>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun updatePollIgnored(poll: PollEntity): Int

    @Query("DELETE FROM $POLL_OPTION_TABLE WHERE pollId = :pollId")
    protected abstract suspend fun deletePollOptions(pollId: String)

    @Query("select exists(select * from $MESSAGE_TABLE where tid =:tid)")
    abstract suspend fun existsMessageByTid(tid: Long): Boolean
}
