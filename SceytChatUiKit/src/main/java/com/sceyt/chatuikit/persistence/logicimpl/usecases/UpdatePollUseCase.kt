package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEventData
import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEventEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toPollEntity
import com.sceyt.chatuikit.persistence.mappers.toPollVoteEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class UpdatePollUseCase(
    private val messageDao: MessageDao,
    private val pollDao: PollDao,
    private val messagesCache: MessagesCache
) {

    suspend operator fun invoke(data: PollUpdateEventData) = withContext(Dispatchers.IO) {
        val (message, votes, event) = data

        when (event) {
            PollUpdateEventEnum.VoteAdded -> {
                handleVoteAdded(message, votes.orEmpty())
            }

            PollUpdateEventEnum.VoteDeleted -> {
                handleVoteDeleted(message, votes.orEmpty())
            }

            PollUpdateEventEnum.VoteRetracted -> {
                handleVoteRetracted(message, votes.orEmpty())
            }

            PollUpdateEventEnum.PollClosed -> {
                handlePollClosed(message)
            }
        }
    }

    private suspend fun handleVoteAdded(message: SceytMessage, votes: List<Vote>) {
        val poll = message.poll ?: return

        // If multiple votes are not allowed, remove previous votes of the user
        if (!poll.allowMultipleVotes) {
            votes.forEach {
                it.user?.id?.let { userId ->
                    pollDao.deleteUserVotes(
                        pollId = poll.id,
                        userId = userId
                    )
                }
            }
        }

        pollDao.upsertPollEntityWithVotes(
            entity = message.poll.toPollEntity(message.tid),
            votes = votes.map { it.toPollVoteEntity(poll.id) }
        )

        getAndUpdateMessageInCache(message)
    }

    private suspend fun handleVoteDeleted(message: SceytMessage, votes: List<Vote>) {
        val poll = message.poll ?: return

        votes.forEach { vote ->
            vote.user?.id?.let { userId ->
                pollDao.deleteUserVote(
                    pollId = poll.id,
                    optionId = vote.optionId,
                    userId = userId
                )
            }
        }

        pollDao.upsertPollEntityWithVotes(
            entity = message.poll.toPollEntity(message.tid),
            votes = emptyList()
        )

        getAndUpdateMessageInCache(message)
    }

    private suspend fun handleVoteRetracted(message: SceytMessage, votes: List<Vote>) {
        handleVoteDeleted(message, votes)
    }

    private suspend fun handlePollClosed(message: SceytMessage) {
        val poll = message.poll ?: return
        pollDao.upsertPollEntityWithVotes(
            entity = poll.toPollEntity(message.tid),
            votes = emptyList()
        )

        getAndUpdateMessageInCache(message)
    }

    private suspend fun getAndUpdateMessageInCache(message: SceytMessage) {
        messageDao.getMessageById(message.id)?.let {
            messagesCache.upsertMessages(message.channelId, it.toSceytMessage())
        }
    }
}