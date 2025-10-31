package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RetractPollVoteUseCase(
        private val pollRepository: PollRepository,
        private val messageDao: MessageDao,
        private val pendingPollVoteDao: PendingPollVoteDao,
        private val messagesCache: MessagesCache,
) {

    suspend operator fun invoke(
            channelId: Long,
            messageTid: Long,
            pollId: String,
    ): SceytResponse<SceytMessage> = withContext(Dispatchers.IO) {
        val message = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
                ?: return@withContext createErrorResponse("Message not found in database")

        val poll = message.poll
                ?: return@withContext createErrorResponse("Poll not found in message")

        // Clear all pending votes for this poll
        pendingPollVoteDao.deletePendingVotesByPollId(messageTid, pollId)

        // If message is pending, update it locally without server call
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            message.copy(poll = poll.copy(
                ownVotes = emptyList(),
                votesPerOption = poll.votesPerOption.mapValues { 0 },
                pendingVotes = null,
            ))
            messagesCache.upsertMessages(channelId, message)
            return@withContext SceytResponse.Success(message)
        }

        // Call server to retract votes
        val response = pollRepository.retractVote(channelId, message.id, pollId)

        // Update cache if successful
        response.onSuccessNotNull { result ->
            messagesCache.upsertMessages(channelId, result)
            messageDao.upsertMessage(result.toMessageDb(false))
        }

        return@withContext response
    }
}

