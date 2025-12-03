package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import com.sceyt.chatuikit.presentation.extensions.isPending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EndPollUseCase(
        private val pollRepository: PollRepository,
        private val messageDao: MessageDao,
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

        // Check if poll is already closed
        if (poll.closed) {
            return@withContext createErrorResponse("Poll is already closed")
        }

        if (message.isPending()) {
            return@withContext createErrorResponse("Cannot end a poll for a pending message")
        }

        // Call server to end poll
        val response = pollRepository.endPoll(channelId, message.id, pollId)

        // Update cache if successful
        response.onSuccessNotNull { result ->
            messagesCache.upsertMessages(channelId, result)
            messageDao.upsertMessage(result.toMessageDb(false))
        }

        return@withContext response
    }
}

