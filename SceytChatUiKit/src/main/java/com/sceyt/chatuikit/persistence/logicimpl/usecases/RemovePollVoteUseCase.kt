package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.repositories.PollRepository

/**
 * Use case for removing a poll vote with server synchronization.
 * Handles database updates and cleans up pending votes after successful API call.
 */
internal class RemovePollVoteUseCase(
        private val pollRepository: PollRepository,
        private val pollDao: PollDao,
        private val messageDao: MessageDao,
        private val pendingPollVoteDao: PendingPollVoteDao,
) {

    /**
     * Removes a vote from a poll option via API and updates database.
     *
     * @param messageTid The message transaction ID
     * @param pollId The poll ID
     * @param optionId The option ID to remove vote from
     * @param userId The user ID whose vote to remove
     * @return Response with updated message or error
     */
    suspend operator fun invoke(
            messageTid: Long,
            pollId: String,
            optionId: String,
            userId: String,
    ): SceytResponse<SceytMessage> {
        val response = pollRepository.deleteVote(pollId, optionId)

        return when (response) {
            is SceytResponse.Success -> {
                // Delete vote directly from database
                pollDao.deleteVote(pollId, optionId, userId)

                // Remove pending vote since operation succeeded
                pendingPollVoteDao.deleteByOption(messageTid, pollId, optionId)

                // Return updated message from database
                val updatedMessage = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
                SceytResponse.Success(updatedMessage)
            }

            is SceytResponse.Error -> response
        }
    }
}

