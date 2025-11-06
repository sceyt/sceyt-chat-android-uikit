package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEvent
import com.sceyt.chatuikit.data.models.ChangeVoteResponseData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity
import com.sceyt.chatuikit.persistence.logic.PersistencePollLogic
import com.sceyt.chatuikit.persistence.logicimpl.usecases.EndPollUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.RetractPollVoteUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPollPendingVotesUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.TogglePollVoteUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UpdatePollUseCase
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class PersistencePollLogicImpl(
    private val messageDao: MessageDao,
    private val pendingPollVoteDao: PendingPollVoteDao,
    private val togglePollVoteUseCase: TogglePollVoteUseCase,
    private val retractPollVoteUseCase: RetractPollVoteUseCase,
    private val endPollUseCase: EndPollUseCase,
    private val updatePollUseCase: UpdatePollUseCase,
    private val sendPollPendingVotesUseCase: SendPollPendingVotesUseCase
) : PersistencePollLogic {

    private val pollUpdateMutex = ConcurrentHashMap<Long, Mutex>()

    override suspend fun toggleVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
        optionId: String,
    ): SceytResponse<ChangeVoteResponseData> {
        pollMutexForMessage(messageTid).withLock {
            val message = messageDao.getMessageByTid(messageTid)
                ?: return createErrorResponse("Message not found in database")

            return togglePollVoteUseCase.invoke(channelId, message.toSceytMessage(), optionId)
        }
    }

    override suspend fun retractVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
    ): SceytResponse<ChangeVoteResponseData> = pollMutexForMessage(messageTid).withLock {
        return retractPollVoteUseCase.invoke(channelId, messageTid, pollId)
    }

    override suspend fun endPoll(
        channelId: Long,
        messageTid: Long,
        pollId: String,
    ): SceytResponse<SceytMessage> {
        pollMutexForMessage(messageTid).withLock {
            return endPollUseCase.invoke(channelId, messageTid, pollId)
        }
    }

    override suspend fun sendAllPendingVotes() {
        val pendingVotes = pendingPollVoteDao.getAllPendingVotes()
        if (pendingVotes.isNotEmpty()) {
            val groupByMessage = pendingVotes.groupBy { it.messageTid }
            coroutineScope {
                for ((messageTid, votes) in groupByMessage) {
                    async {
                        sendPendingVotesSync(messageTid, votes)
                    }
                }
            }
        }
    }

    override suspend fun onPollUpdated(event: PollUpdateEvent) {
        pollMutexForMessage(event.messageTid).withLock {
            updatePollUseCase(event)
        }
    }

    private suspend fun sendPendingVotesSync(
        messageTid: Long,
        votes: List<PendingPollVoteEntity>
    ) = pollMutexForMessage(messageTid).withLock {
        val messageDb = messageDao.getMessageByTid(messageTid) ?: return
        val poll = messageDb.poll ?: return
        sendPollPendingVotesUseCase(
            channelId = messageDb.messageEntity.channelId,
            messageId = messageDb.id ?: return,
            pollId = poll.pollEntity.id,
            pendingVotes = votes
        )
    }

    private fun pollMutexForMessage(messageTid: Long): Mutex {
        return pollUpdateMutex.computeIfAbsent(messageTid) { Mutex() }
    }
}

