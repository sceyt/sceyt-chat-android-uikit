package com.sceyt.sceytchatuikit.persistence.logics.reactionslogic

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventData
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventEnum.ADD
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventEnum.REMOVE
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.ReactionsRepository
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.ChatUsersReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.PendingReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.PendingReactionEntity
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChatReactionMessagesCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.persistence.mappers.toChannel
import com.sceyt.sceytchatuikit.persistence.mappers.toMessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.toReaction
import com.sceyt.sceytchatuikit.persistence.mappers.toReactionEntity
import com.sceyt.sceytchatuikit.persistence.mappers.toReactionTotalEntity
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import com.sceyt.sceytchatuikit.persistence.mappers.toUserReactionsEntity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class PersistenceReactionsLogicImpl(
        private val reactionsRepository: ReactionsRepository,
        private var messageDao: MessageDao,
        private var usersDao: UserDao,
        private var reactionDao: ReactionDao,
        private var channelReactionsDao: ChatUsersReactionDao,
        private var channelDao: ChannelDao,
        private var pendingReactionDao: PendingReactionDao,
        private var channelsCache: ChannelsCache,
        private var messagesCache: MessagesCache) : PersistenceReactionsLogic {

    override suspend fun onMessageReactionUpdated(data: ReactionUpdateEventData) {
        val messageId = data.message.id
        reactionDao.deleteAllReactionTotalsByMessageId(messageId)
        val existMessage = messageDao.existsMessageById(messageId)
        if (!existMessage)
            messageDao.upsertMessage(data.message.toMessageDb(false))

        when (data.eventType) {
            ADD -> reactionDao.insertReaction(data.reaction.toReactionEntity())
            REMOVE -> reactionDao.deleteReaction(messageId, data.reaction.key, data.reaction.user.id)
        }
        data.message.reactionTotals?.map { it.toReactionTotalEntity(messageId) }?.let {
            reactionDao.insertReactionTotals(it)
        }

        val message = messageDao.getMessageById(messageId)?.toSceytMessage() ?: data.message
        messagesCache.messageUpdated(data.message.channelId, message)
        handleChannelReaction(data, message)
    }

    private suspend fun handleChannelReaction(data: ReactionUpdateEventData, message: SceytMessage) {
        if (data.message.incoming) return

        when (data.eventType) {
            ADD -> {
                channelReactionsDao.insertChannelUserReaction(data.reaction.toUserReactionsEntity(message.channelId))
                ChatReactionMessagesCache.addMessage(message)
            }

            REMOVE -> channelReactionsDao.deleteChannelUserReaction(message.channelId, message.id,
                data.reaction.key, data.reaction.user.id)
        }

        channelDao.getChannelById(message.channelId)?.toChannel()?.let {
            channelsCache.upsertChannel(it)
        }
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

                saveReactionsToDb(reactions)

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

    override suspend fun addReaction(channelId: Long, messageId: Long, key: String, score: Int): SceytResponse<SceytMessage> {
        insertPendingReactionToDb(channelId, messageId, key, true)
        return addReactionImpl(channelId, messageId, key, score, true)
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, key: String, isPending: Boolean): SceytResponse<SceytMessage> {
        if (isPending)
            return removePendingReaction(channelId, messageId, key)

        insertPendingReactionToDb(channelId, messageId, key, false)
        return deleteReactionImpl(channelId, messageId, key, true)
    }

    override suspend fun sendAllPendingReactions() {
        val pendingReactions = pendingReactionDao.getAllData()
        if (pendingReactions.isNotEmpty()) {
            val groupByChannel = pendingReactions.groupBy { it.channelId }
            for ((channelId, reactions) in groupByChannel) {
                sendPendingReactionsSync(channelId, reactions)
            }
        }
    }

    private suspend fun sendPendingReactionsSync(channelId: Long, reactions: List<PendingReactionEntity>) {
        reactions.groupBy { it.messageId }.forEach { (messageId, reactions) ->
            reactions.forEachIndexed { index, pendingReactionEntity ->
                if (pendingReactionEntity.isAdd) {
                    addReactionImpl(channelId, messageId, pendingReactionEntity.key, pendingReactionEntity.score, index == reactions.lastIndex)
                } else
                    deleteReactionImpl(channelId, messageId, pendingReactionEntity.key, index == reactions.lastIndex)
            }
        }
    }

    private suspend fun addReactionImpl(channelId: Long, messageId: Long, key: String, score: Int, emitUpdate: Boolean): SceytResponse<SceytMessage> {
        val response = reactionsRepository.addReaction(channelId, messageId, key, score)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                resultMessage.userReactions?.let {
                    messageDao.insertReactions(it.map { reaction -> reaction.toReactionEntity() })
                }
                resultMessage.reactionTotals?.let {
                    messageDao.insertReactionTotals(it.map { total -> total.toReactionTotalEntity(messageId) })
                }

                messageDao.getMessageTidById(messageId)?.let { tid ->
                    messagesCache.deletePendingReaction(channelId, tid, key)
                }
                pendingReactionDao.deletePendingReaction(messageId, key)

                if (emitUpdate) {
                    val message = messageDao.getMessageById(messageId)?.toSceytMessage()
                            ?: response.data

                    messagesCache.messageUpdated(channelId, message)

                    if (!message.incoming) {
                        val reaction = message.userReactions?.maxBy { it.createdAt }
                        if (reaction != null)
                            handleChannelReaction(ReactionUpdateEventData(message, reaction, ADD), message)
                    }
                }
            }
        }
        return response
    }

    private suspend fun deleteReactionImpl(channelId: Long, messageId: Long, key: String, emitUpdate: Boolean): SceytResponse<SceytMessage> {
        val response = reactionsRepository.deleteReaction(channelId, messageId, key)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                SceytKitClient.myId?.let { fromId ->
                    reactionDao.deleteReactionAndTotal(messageId, key, fromId)
                }
                pendingReactionDao.deletePendingReaction(messageId, key)

                if (emitUpdate) {
                    val message = messageDao.getMessageById(messageId)?.toSceytMessage()
                            ?: resultMessage

                    messagesCache.messageUpdated(channelId, message)

                    if (!message.incoming) {
                        val reaction = Reaction(0, messageId, key, 1, "", 0, ClientWrapper.currentUser)
                        handleChannelReaction(ReactionUpdateEventData(message, reaction, REMOVE), message)
                    }
                }
            }
        }
        return response
    }

    private suspend fun insertPendingReactionToDb(channelId: Long, messageId: Long, key: String, isAdd: Boolean) {
        val messageDb = messageDao.getMessageById(messageId) ?: return
        val pendingReactions = messageDb.pendingReactions?.toArrayList() ?: arrayListOf()

        var pendingReactionEntity = pendingReactions.find { it.key == key }
        if (pendingReactionEntity != null) {
            if (pendingReactionEntity.isAdd != isAdd) {
                pendingReactions.remove(pendingReactionEntity)
                pendingReactionDao.deletePendingReaction(messageId, key)
                pendingReactionEntity = null
            }

        } else {
            val entity = PendingReactionEntity(messageId = messageId, channelId = channelId, key = key,
                score = 1, createdAt = System.currentTimeMillis(), isAdd = isAdd, count = 1)
            pendingReactions.add(entity)
            pendingReactionEntity = entity
        }

        val groupReactions = pendingReactions.groupBy { it.isAdd }
        messageDb.pendingReactions = groupReactions[true]

        val removeReactions = groupReactions[false]
        if (!removeReactions.isNullOrEmpty()) {
            val newTotals = messageDb.reactionsTotals?.toArrayList()?.apply {
                removeAll(filter {
                    removeReactions.any { removeItem -> removeItem.key == it.key }
                }.toSet())
            }
            messageDb.reactionsTotals = newTotals
        }
        val message = messageDb.toSceytMessage()

        messagesCache.messageUpdated(channelId, message)

        pendingReactionEntity?.let { pendingReactionDao.insert(it) }
    }

    private suspend fun removePendingReaction(channelId: Long, messageId: Long, key: String): SceytResponse<SceytMessage> {
        val tid = messageDao.getMessageTidById(messageId)
                ?: return SceytResponse.Error(SceytException(0, "Message not found"))

        pendingReactionDao.deletePendingReaction(messageId, key)
        messagesCache.deletePendingReaction(channelId, tid, key)?.let {
            messagesCache.messageUpdated(channelId, it)
        }

        return SceytResponse.Success(null)
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

    private suspend fun saveReactionsToDb(list: List<Reaction>) {
        if (list.isEmpty()) return

        reactionDao.insertReactions(list.map { it.toReactionEntity() })
        usersDao.insertUsers(list.mapNotNull { it.user?.toUserEntity() })
    }
}