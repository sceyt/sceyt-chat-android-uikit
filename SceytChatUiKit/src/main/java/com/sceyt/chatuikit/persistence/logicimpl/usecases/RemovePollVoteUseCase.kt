package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for removing a poll vote with server synchronization.
 * Handles database updates and cleans up pending votes after successful API call.
 */
internal class RemovePollVoteUseCase(
    private val pollRepository: PollRepository,
    private val messagesCache: MessagesCache,
    private val messageDao: MessageDao,
    private val pendingPollVoteDao: PendingPollVoteDao,
) {

    /**
     * Removes a vote from a poll option via API and updates database.
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @param pollId The poll ID
     * @param optionId The option ID to remove vote from
     * @return Response with updated message or error
     */
    suspend operator fun invoke(
        channelId: ChannelId,
        messageId: Long,
        pollId: String,
        optionId: String,
    ): SceytResponse<SceytMessage> = withContext(Dispatchers.IO) {
        return@withContext pollRepository.deleteVote(
            channelId = channelId,
            messageId = messageId,
            pollId = pollId,
            optionId = optionId
        ).onSuccessNotNull { message ->
            // Remove pending vote since operation succeeded
            pendingPollVoteDao.deleteByOption(
                messageTid = message.tid,
                pollId = pollId,
                optionId = optionId
            )

            messageDao.upsertMessage(messageDb = message.toMessageDb(false))

            messageDao.getMessageById(messageId)?.let {
                messagesCache.upsertMessages(channelId, it.toSceytMessage())
            }
        }
    }
}

