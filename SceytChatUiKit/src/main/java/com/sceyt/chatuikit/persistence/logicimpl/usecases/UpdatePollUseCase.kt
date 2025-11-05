package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEvent
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toPollEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class UpdatePollUseCase(
    private val messageDao: MessageDao,
    private val pollDao: PollDao,
    private val messagesCache: MessagesCache,
    private val updatePollVotesUseCase: UpdatePollVotesUseCase
) {

    suspend operator fun invoke(event: PollUpdateEvent) = withContext(Dispatchers.IO) {
        when (event) {
            is PollUpdateEvent.VoteChanged -> {
                handleVotesChanged(
                    message = event.message,
                    addedVoted = event.addedVotes,
                    removedVotes = event.removedVotes
                )
            }

            is PollUpdateEvent.VoteRetracted -> {
                handleVoteRetracted(event.message, event.retractedVotes)
            }

            is PollUpdateEvent.PollClosed -> {
                handlePollClosed(event.message)
            }
        }
    }

    private suspend fun handleVotesChanged(
        message: SceytMessage,
        addedVoted: List<Vote>,
        removedVotes: List<Vote>
    ) {
        updatePollVotesUseCase(
            message = message,
            addedVoted = addedVoted,
            removedVotes = removedVotes
        )
    }

    private suspend fun handleVoteRetracted(message: SceytMessage, votes: List<Vote>) {
        updatePollVotesUseCase(message = message, addedVoted = emptyList(), removedVotes = votes)
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