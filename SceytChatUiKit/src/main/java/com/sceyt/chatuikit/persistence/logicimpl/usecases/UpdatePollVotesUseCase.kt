package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toPollVoteEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlin.collections.forEach

internal class UpdatePollVotesUseCase(
    private val pollDao: PollDao,
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache
) {

    suspend operator fun invoke(
        message: SceytMessage,
        addedVoted: List<Vote>,
        removedVotes: List<Vote>
    ) {
        val pollId = message.poll?.id ?: return
        val pollEntity = pollDao.getPollEntityById(message.tid, pollId) ?: return

        val votedPorOption = pollEntity.votesPerOption.toMutableMap()

        // Update votes per option count
        addedVoted.forEach { vote ->
            votedPorOption[vote.optionId] = (votedPorOption[vote.optionId] ?: 0) + 1
        }

        removedVotes.forEach { vote ->
            votedPorOption[vote.optionId]?.let { voteCount ->
                if (voteCount > 0)
                    votedPorOption[vote.optionId] = voteCount - 1
                else {
                    votedPorOption.remove(vote.optionId)
                }
            }
        }

        val updatedPollEntity = pollEntity.copy(votesPerOption = votedPorOption)

        pollDao.upsertPollEntityWithVotes(
            entity = updatedPollEntity,
            addedVotes = addedVoted.map { it.toPollVoteEntity(pollId) },
            deletedVotes = removedVotes.map { it.toPollVoteEntity(pollId) }
        )

        getAndUpdateMessageInCache(message)
    }


    private suspend fun getAndUpdateMessageInCache(message: SceytMessage) {
        messageDao.getMessageById(message.id)?.let {
            messagesCache.upsertMessages(message.channelId, it.toSceytMessage())
        }
    }
}