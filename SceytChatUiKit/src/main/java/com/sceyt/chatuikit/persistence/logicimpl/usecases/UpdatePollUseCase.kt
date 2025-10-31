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

internal class UpdatePollUseCase(
    private val messageDao: MessageDao,
    private val pollDao: PollDao,
    private val messagesCache: MessagesCache
) {

    suspend operator fun invoke(data: PollUpdateEventData) {
        val (message, votes, event) = data

        when (event) {
            PollUpdateEventEnum.VoteAdded -> {
                handleVoteAdded(message, votes.orEmpty())
            }

            PollUpdateEventEnum.VoteDeleted -> {
                handleVoteDeleted(message, votes.orEmpty())
            }

            PollUpdateEventEnum.VoteRetracted -> TODO()
            PollUpdateEventEnum.PollClosed -> TODO()
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

        messageDao.getMessageById(message.id)?.let {
            messagesCache.upsertMessages(message.channelId, it.toSceytMessage())
        }
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
            votes =emptyList()
        )

        messageDao.getMessageById(message.id)?.let {
            messagesCache.upsertMessages(message.channelId, it.toSceytMessage())
        }
    }
}