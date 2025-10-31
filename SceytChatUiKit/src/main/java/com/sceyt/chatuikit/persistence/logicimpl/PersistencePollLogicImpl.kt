package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEventData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity
import com.sceyt.chatuikit.persistence.logic.PersistencePollLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.usecases.EndPollUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.RetractPollVoteUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.TogglePollVoteUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UpdatePollUseCase
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PersistencePollLogicImpl(
    private val messageDao: MessageDao,
    private val pendingPollVoteDao: PendingPollVoteDao,
    private val messagesCache: MessagesCache,
    private val togglePollVoteUseCase: TogglePollVoteUseCase,
    private val retractPollVoteUseCase: RetractPollVoteUseCase,
    private val endPollUseCase: EndPollUseCase,
    private val updatePollUseCase: UpdatePollUseCase,
) : PersistencePollLogic {

    private val pollUpdateMutex = Mutex()

    override suspend fun toggleVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
        optionId: String,
    ): SceytResponse<SceytMessage> {
        pollUpdateMutex.withLock {
            val message = messageDao.getMessageByTid(messageTid)
                ?: return createErrorResponse("Message not found in database")

            return togglePollVoteUseCase.invoke(channelId, message.toSceytMessage(), optionId)
        }
    }

    override suspend fun retractVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
    ): SceytResponse<SceytMessage> {
        pollUpdateMutex.withLock {
            return retractPollVoteUseCase.invoke(channelId, messageTid, pollId)
        }
    }

    override suspend fun endPoll(
        channelId: Long,
        messageTid: Long,
        pollId: String,
    ): SceytResponse<SceytMessage> {
        pollUpdateMutex.withLock {
            return endPollUseCase.invoke(channelId, messageTid, pollId)
        }
    }

    override suspend fun sendAllPendingVotes() {
        val pendingVotes = pendingPollVoteDao.getAllPendingVotes()
        if (pendingVotes.isNotEmpty()) {
            val groupByMessage = pendingVotes.groupBy { it.messageTid }
            for ((messageTid, votes) in groupByMessage) {
                sendPendingVotesSync(messageTid, votes)
            }
        }
    }

    override suspend fun onPollUpdated(eventData: PollUpdateEventData) {
        pollUpdateMutex.withLock {
            updatePollUseCase(eventData)
        }
    }

    private suspend fun sendPendingVotesSync(
        messageTid: Long,
        votes: List<PendingPollVoteEntity>
    ) {
        val messageDb = messageDao.getMessageByTid(messageTid) ?: return
        val channelId = messageDb.messageEntity.channelId

        /*  votes.forEach { entity ->
              if (entity.isAdd) {
                  addPollVoteUseCase(
                      channelId = channelId,
                      message = messageDb.toSceytMessage(),
                      optionId = entity.optionId
                  )
              } else {
                  removePollVoteUseCase(messageTid, entity.pollId, entity.optionId, entity.userId)
              }
              // Update cache with fresh data from database after each vote
              updateCacheAfterPendingChange(channelId, messageTid)
          }*/
    }

    private suspend fun updateCacheAfterPendingChange(channelId: Long, messageTid: Long) {
        // Reload message from DB (includes updated pending votes automatically)
        val updatedMessage = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
        if (updatedMessage != null) {
            messagesCache.messageUpdated(channelId, updatedMessage)
        }
    }
}

