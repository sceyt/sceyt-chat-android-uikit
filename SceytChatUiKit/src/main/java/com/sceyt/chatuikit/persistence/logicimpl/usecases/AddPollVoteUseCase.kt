package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.ChangeVoteResponseData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for adding a poll vote with server synchronization.
 * Handles database updates and cleans up pending votes after successful API call.
 */
internal class AddPollVoteUseCase(
    private val pollRepository: PollRepository,
    private val updatePollVotesUseCase: UpdatePollVotesUseCase,
    private val pendingPollVoteDao: PendingPollVoteDao
) {

    /**
     * Adds a vote to a poll option via API and updates database.
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @param pollId The poll ID
     * @param optionIds The option IDs to vote for
     * @return Response with updated message or error
     */
    suspend operator fun invoke(
        channelId: ChannelId,
        messageId: Long,
        pollId: String,
        optionIds: List<String>,
    ): SceytResponse<ChangeVoteResponseData> = withContext(Dispatchers.IO) {
        return@withContext pollRepository.changeVotes(
            channelId = channelId,
            messageId = messageId,
            pollId = pollId,
            addOptionIds = optionIds,
            removeOptionIds = emptyList()
        ).onSuccessNotNull { (message, addedVotes, removedVotes) ->
            // Update message in database
            updatePollVotesUseCase.invoke(
                message = message,
                addedVoted = addedVotes,
                removedVotes = removedVotes
            )
        }.onError {
            // Handle specific error codes if needed
            if (it?.code == 1301) {
                pendingPollVoteDao.deleteVotesByOptionIds(
                    messageTid = messageId,
                    pollId = pollId,
                    optionIds = optionIds
                )
            }
        }
    }
}