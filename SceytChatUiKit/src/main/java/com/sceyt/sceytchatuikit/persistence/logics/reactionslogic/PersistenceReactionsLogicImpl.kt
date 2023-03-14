package com.sceyt.sceytchatuikit.persistence.logics.reactionslogic

import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventData
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventEnum
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.ReactionsRepository
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.persistence.mappers.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class PersistenceReactionsLogicImpl(
        private val reactionsRepository: ReactionsRepository,
        private var messageDao: MessageDao,
        private var usersDao: UserDao,
        private var reactionDao: ReactionDao,
        private val preference: SceytSharedPreference,
        private var messagesCache: MessagesCache) : PersistenceReactionsLogic {

    override suspend fun onMessageReactionUpdated(data: ReactionUpdateEventData) {
        val messageId = data.message.id
        reactionDao.deleteAllReactionScoresByMessageId(messageId)

        when (data.eventType) {
            ReactionUpdateEventEnum.ADD -> reactionDao.insertReaction(data.reaction.toReactionEntity(messageId))
            ReactionUpdateEventEnum.REMOVE -> reactionDao.deleteReactionByIds(data.reaction.id)
        }
        reactionDao.insertReactionScores(data.message.reactionScores.map { it.toReactionScoreEntity(messageId) })

        val message = messageDao.getMessageById(messageId)?.toSceytMessage()
                ?: data.message.toSceytUiMessage()

        messagesCache.messageUpdated(message)
    }

    override suspend fun loadReactions(messageId: Long, offset: Int, key: String, loadKey: LoadKeyData?, ignoreDb: Boolean): Flow<PaginationResponse<Reaction>> {
        return callbackFlow {

            val dbReactions = getReactionsDb(messageId, offset, SceytKitConfig.REACTIONS_LOAD_SIZE, key)
            var hasNext = dbReactions.size == SceytKitConfig.REACTIONS_LOAD_SIZE

            trySend(PaginationResponse.DBResponse(data = dbReactions, loadKey = loadKey, offset = offset,
                hasNext = hasNext, hasPrev = false))

            ConnectionEventsObserver.awaitToConnectSceyt()

            val response = if (offset == 0) reactionsRepository.getReactions(messageId, key)
            else reactionsRepository.loadMoreReactions(messageId, key)

            if (response is SceytResponse.Success) {
                val reactions = response.data ?: arrayListOf()

                val deletedReactions = dbReactions.filter { dbReaction ->
                    reactions.none { it.id == dbReaction.id }
                }

                reactionDao.deleteReactionByIds(*deletedReactions.map { it.id }.toLongArray())

                saveReactionsToDb(messageId, reactions)

                val limit = SceytKitConfig.REACTIONS_LOAD_SIZE + offset
                val cashData = getReactionsDb(messageId, 0, limit, key)
                hasNext = response.data?.size == SceytKitConfig.REACTIONS_LOAD_SIZE

                trySend(PaginationResponse.ServerResponse(data = response, cacheData = cashData,
                    loadKey = loadKey, offset = offset, hasDiff = true, hasNext = hasNext, hasPrev = false,
                    loadType = PaginationResponse.LoadType.LoadNext, ignoredDb = ignoreDb))
            }

            channel.close()
            awaitClose()
        }
    }

    override suspend fun getMessageReactionsDbByKey(messageId: Long, key: String): List<Reaction> {
        return if (key.isEmpty())
            reactionDao.getReactionsByMsgId(messageId).map { it.toReaction() }
        else
            reactionDao.getReactionsByMsgIdAndKey(messageId, key).map { it.toReaction() }
    }

    override suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        val response = reactionsRepository.addReaction(channelId, messageId, scoreKey)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                message.selfReactions?.let {
                    messageDao.insertReactions(it.map { reaction -> reaction.toReactionEntity(messageId) })
                }
                message.reactionScores?.let {
                    messageDao.insertReactionScores(it.map { score -> score.toReactionScoreEntity(messageId) })
                }
                messagesCache.messageUpdated(message)
            }
        }
        return response
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        val response = reactionsRepository.deleteReaction(channelId, messageId, scoreKey)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                message.reactionScores?.let {
                    val fromId = preference.getUserId()
                    if (fromId != null)
                        reactionDao.deleteReactionAndScore(messageId, scoreKey, fromId)
                }
                messagesCache.messageUpdated(message)
            }
        }
        return response
    }

    private suspend fun getReactionsDb(messageId: Long, offset: Int, limit: Int, key: String): List<Reaction> {
        return if (key.isBlank()) {
            reactionDao.getReactions(messageId = messageId, limit = limit,
                offset = offset).map { it.toReaction() }
        } else {
            reactionDao.getReactionsByKey(messageId = messageId, limit = limit,
                offset = offset, key = key).map { it.toReaction() }
        }
    }

    private suspend fun saveReactionsToDb(messageId: Long, list: List<Reaction>) {
        if (list.isEmpty()) return

        reactionDao.insertReactions(list.map { it.toReactionEntity(messageId) })
        usersDao.insertUsers(list.mapNotNull { it.user?.toUserEntity() })
    }
}