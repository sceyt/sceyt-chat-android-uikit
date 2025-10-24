package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPollVoteEntity
import com.sceyt.chatuikit.persistence.logic.PersistencePollLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.usecases.TogglePollVoteUseCase
import com.sceyt.chatuikit.persistence.mappers.getTid
import com.sceyt.chatuikit.persistence.mappers.toPollDb
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PersistencePollLogicImpl(
        private val messageDao: MessageDao,
        private val pollDao: PollDao,
        private val pendingPollVoteDao: PendingPollVoteDao,
        private val messagesCache: MessagesCache,
        private val togglePollVoteUseCase: TogglePollVoteUseCase,
) : PersistencePollLogic {

    private val pollUpdateMutex = Mutex()

    override suspend fun toggleVote(
            channelId: Long,
            messageTid: Long,
            pollId: String,
            optionId: String,
    ): SceytResponse<SceytMessage> {
        pollUpdateMutex.withLock {
            val message = messageDao.getMessageByTid(messageTid) ?: return SceytResponse.Error(
                SceytException(0, "Message not found in database")
            )

            return togglePollVoteUseCase.invoke(channelId, message.toSceytMessage(), optionId)
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

    override suspend fun onPollUpdated(message: SceytMessage) {
        pollUpdateMutex.withLock {
            val poll = message.poll ?: return
            val messageTid = getTid(message.id, message.tid, message.incoming)

            // Check if message exists
            val existsMessage = messageDao.existsMessageByTid(messageTid)
            if (!existsMessage) return

            // Update poll in database
            pollDao.upsertPoll(poll.toPollDb())

            // Update message in cache
            val updatedMessage = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
            if (updatedMessage != null) {
                messagesCache.messageUpdated(message.channelId, updatedMessage)
            }
        }
    }

    private suspend fun sendPendingVotesSync(messageTid: Long, votes: List<PendingPollVoteEntity>) {
        val messageDb = messageDao.getMessageByTid(messageTid) ?: return
        val channelId = messageDb.messageEntity.channelId

        /*votes.forEach { entity ->
            if (entity.isAdd) {
                addPollVoteUseCase(messageTid, entity.pollId, entity.optionId)
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

