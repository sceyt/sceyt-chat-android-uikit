package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventData
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventEnum.Add
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventEnum.Remove
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.database.dao.ReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingReactionEntity
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChatReactionMessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toReactionEntity
import com.sceyt.chatuikit.persistence.mappers.toReactionTotalEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.persistence.mappers.toUserReactionsEntity
import com.sceyt.chatuikit.persistence.repositories.ReactionsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PersistenceReactionsLogicImpl(
    private val reactionsRepository: ReactionsRepository,
    private var messageDao: MessageDao,
    private var usersDao: UserDao,
    private var reactionDao: ReactionDao,
    private var channelReactionsDao: ChatUserReactionDao,
    private var channelDao: ChannelDao,
    private var pendingReactionDao: PendingReactionDao,
    private var channelsCache: ChannelsCache,
    private var messagesCache: MessagesCache
) : PersistenceReactionsLogic {

    private val reactionUpdateMutex = Mutex()
    private val reactionsLoadSize get() = SceytChatUIKit.config.queryLimits.reactionListQueryLimit

    override suspend fun onMessageReactionUpdated(data: ReactionUpdateEventData) {
        reactionUpdateMutex.withLock {
            val messageId = data.message.id
            val existMessage = messageDao.existsMessageById(messageId)
            if (!existMessage)
                messageDao.upsertMessage(data.message.toMessageDb(false))

            when (data.eventType) {
                Add -> reactionDao.insertReactionAndIncreaseTotalIfNeeded(
                    entity = data.reaction.toReactionEntity()
                )

                Remove -> reactionDao.deleteReactionAndTotal(
                    messageId = messageId,
                    key = data.reaction.key,
                    fromId = data.reaction.user?.id,
                    score = data.reaction.score
                )
            }

            val message = messageDao.getMessageById(messageId)?.toSceytMessage() ?: data.message
            messagesCache.messageUpdated(data.message.channelId, message)
            handleChannelReaction(data, message)
        }
    }

    private suspend fun handleChannelReaction(
        data: ReactionUpdateEventData,
        message: SceytMessage
    ) {
        if (data.message.incoming) return

        when (data.eventType) {
            Add -> {
                if (!message.incoming) {
                    channelReactionsDao.insertChannelUserReaction(
                        reaction = data.reaction.toUserReactionsEntity(message.channelId)
                    )
                    ChatReactionMessagesCache.addMessage(message)
                }
            }

            Remove -> channelReactionsDao.deleteChannelUserReaction(
                channelId = message.channelId,
                messageId = message.id,
                key = data.reaction.key,
                fromId = data.reaction.user?.id
            )
        }

        channelDao.getChannelById(message.channelId)?.toChannel()?.let {
            channelsCache.upsertChannel(it)
        }
    }

    override suspend fun loadReactions(
        messageId: Long,
        offset: Int,
        key: String,
        loadKey: LoadKeyData?,
        ignoreDb: Boolean
    ): Flow<PaginationResponse<SceytReaction>> {
        return callbackFlow {

            var dbReactions = getReactionsDb(messageId, offset, reactionsLoadSize, key)
            var hasNext = dbReactions.size == reactionsLoadSize

            dbReactions = dbReactions.updateWithPendingReactions(messageId, key)

            trySend(
                PaginationResponse.DBResponse(
                    data = dbReactions,
                    loadKey = loadKey,
                    offset = offset,
                    hasNext = hasNext,
                    hasPrev = false
                )
            )

            ConnectionEventManager.awaitToConnectSceyt()

            val response = if (offset == 0) reactionsRepository.getReactions(messageId, key)
            else reactionsRepository.loadMoreReactions(messageId, key)

            if (response is SceytResponse.Success) {
                val reactions = response.data ?: arrayListOf()

                val deletedReactions = dbReactions.filter { dbReaction ->
                    !dbReaction.pending && reactions.none { it.id == dbReaction.id }
                }

                reactionDao.deleteReactionByIds(*deletedReactions.map { it.id }.toLongArray())

                saveReactionsToDb(reactions)

                val limit = reactionsLoadSize + offset
                val cashData = getReactionsDb(messageId, 0, limit, key).updateWithPendingReactions(
                    messageId = messageId,
                    key = key
                )

                hasNext = response.data?.size == reactionsLoadSize

                trySend(
                    PaginationResponse.ServerResponse(
                        data = response,
                        cacheData = cashData,
                        loadKey = loadKey,
                        offset = offset,
                        hasDiff = true,
                        hasNext = hasNext,
                        hasPrev = false,
                        loadType = PaginationResponse.LoadType.LoadNext,
                        ignoredDb = ignoreDb
                    )
                )
            }

            channel.close()
            awaitClose()
        }
    }

    private suspend fun List<SceytReaction>.updateWithPendingReactions(
        messageId: Long,
        key: String
    ): List<SceytReaction> {
        val pendingData = (if (key.isBlank()) pendingReactionDao.getAllByMsgId(messageId)
        else pendingReactionDao.getAllByMsgIdAndKey(messageId, key)).groupBy { it.isAdd }

        var dbReactions = ArrayList(this)
        val pendingAddedR = pendingData[true]
        val pendingRemoveItems = pendingData[false]

        if (!pendingRemoveItems.isNullOrEmpty()) {
            dbReactions.apply {
                val needTOBeRemoved = dbReactions.filter { reaction ->
                    pendingRemoveItems.any { it.key == reaction.key && reaction.user?.id == SceytChatUIKit.chatUIFacade.myId }
                }
                removeAll(needTOBeRemoved.toSet())
            }
        }

        if (!pendingAddedR.isNullOrEmpty()) {
            dbReactions = dbReactions.toArrayList().apply {
                addAll(0, pendingAddedR.map { it.toSceytReaction() })
            }
        }
        return dbReactions
    }

    override suspend fun getLocalMessageReactionsById(reactionId: Long): SceytReaction? {
        return reactionDao.getReactionsById(reactionId)?.toSceytReaction()
    }

    override suspend fun getLocalMessageReactionsByKey(
        messageId: Long,
        key: String
    ): List<SceytReaction> {
        return if (key.isEmpty())
            reactionDao.getReactionsByMsgId(messageId).map { it.toSceytReaction() }
        else
            reactionDao.getReactionsByMsgIdAndKey(messageId, key).map { it.toSceytReaction() }
    }

    override suspend fun addReaction(
        channelId: Long, messageId: Long, key: String, score: Int,
        reason: String, enforceUnique: Boolean
    ): SceytResponse<SceytMessage> {
        reactionUpdateMutex.withLock {
            val wasPending = insertPendingReactionToDbAndGetWasPending(
                channelId = channelId,
                messageId = messageId,
                key = key,
                reason = reason,
                enforceUnique = enforceUnique,
                isAdd = true
            )
            if (wasPending)
                return SceytResponse.Success(null)

            return addReactionImpl(
                channelId = channelId,
                messageId = messageId,
                key = key,
                reason = reason,
                score = score,
                enforceUnique = enforceUnique,
                emitUpdate = true
            )
        }
    }

    override suspend fun deleteReaction(
        channelId: Long,
        messageId: Long,
        key: String
    ): SceytResponse<SceytMessage> {
        reactionUpdateMutex.withLock {
            val wasPending = insertPendingReactionToDbAndGetWasPending(
                channelId = channelId,
                messageId = messageId,
                key = key,
                reason = "",
                enforceUnique = true,
                isAdd = false
            )
            if (wasPending)
                return SceytResponse.Success(null)

            return deleteReactionImpl(channelId, messageId, key, true)
        }
    }

    override suspend fun sendAllPendingReactions() {
        val pendingReactions = pendingReactionDao.getAll()
        if (pendingReactions.isNotEmpty()) {
            val groupByChannel = pendingReactions.groupBy { it.channelId }
            for ((channelId, reactions) in groupByChannel) {
                sendPendingReactionsSync(channelId, reactions)
            }
        }
    }

    private suspend fun sendPendingReactionsSync(
        channelId: Long,
        reactions: List<PendingReactionEntity>
    ) {
        reactions.groupBy { it.messageId }.forEach { (messageId, reactions) ->
            reactions.forEachIndexed { index, entity ->
                if (entity.isAdd) {
                    addReactionImpl(
                        channelId = channelId,
                        messageId = messageId,
                        key = entity.key,
                        reason = entity.reason,
                        score = entity.score,
                        enforceUnique = entity.enforceUnique,
                        emitUpdate = index == reactions.lastIndex
                    )
                } else
                    deleteReactionImpl(
                        channelId = channelId,
                        messageId = messageId,
                        key = entity.key,
                        emitUpdate = index == reactions.lastIndex
                    )
            }
        }
    }

    private suspend fun addReactionImpl(
        channelId: Long,
        messageId: Long,
        key: String,
        reason: String,
        score: Int,
        enforceUnique: Boolean,
        emitUpdate: Boolean
    ): SceytResponse<SceytMessage> {
        val response = reactionsRepository.addReaction(
            channelId = channelId,
            messageId = messageId,
            key = key,
            score = score,
            reason = reason,
            enforceUnique = enforceUnique
        ).onSuccessNotNull { resultMessage ->
            reactionDao.insertMessageReactionsAndTotalsIfMessageExist(
                messageId = messageId,
                reactions = resultMessage.userReactions?.map { it.toReactionEntity() },
                reactionTotals = resultMessage.reactionTotals?.map {
                    it.toReactionTotalEntity(messageId)
                }
            )

            messagesCache.deletePendingReaction(channelId, resultMessage.tid, key)
            pendingReactionDao.deletePendingReaction(messageId, key)

            if (emitUpdate) {
                val message =
                    messageDao.getMessageById(messageId)?.toSceytMessage() ?: resultMessage

                messagesCache.messageUpdated(channelId, message)
                ChatReactionMessagesCache.addMessage(message)

                if (!message.incoming) {
                    val reaction = message.userReactions?.maxByOrNull { it.createdAt }
                    if (reaction != null)
                        handleChannelReaction(
                            data = ReactionUpdateEventData(message, reaction, Add),
                            message = message
                        )
                }
            }
        }.onError { exception ->
            val errorType = SDKErrorTypeEnum.fromValue(exception?.type) ?: return@onError
            if (!errorType.isResendable) {
                SceytLog.e(
                    TAG,
                    "Add reaction error: ${exception?.message}, errorType: ${errorType.value}" +
                            "channel id $channelId, reaction:${key} will be deleted due to non-resendable error"
                )
                pendingReactionDao.deletePendingReaction(messageId, key)
                messagesCache.deletePendingReaction(channelId, messageId, key)
            }
        }
        return response
    }

    private suspend fun deleteReactionImpl(
        channelId: Long,
        messageId: Long,
        key: String,
        emitUpdate: Boolean
    ): SceytResponse<SceytMessage> {
        val response = reactionsRepository.deleteReaction(channelId, messageId, key)
        response.onSuccessNotNull { resultMessage ->
            reactionDao.deleteAllReactionsAndTotals(messageId)
            reactionDao.insertMessageReactionsAndTotalsIfMessageExist(
                messageId = messageId,
                reactions = resultMessage.userReactions?.map { it.toReactionEntity() },
                reactionTotals = resultMessage.reactionTotals?.map {
                    it.toReactionTotalEntity(messageId)
                }
            )

            pendingReactionDao.deletePendingReaction(messageId, key)
            messagesCache.deletePendingReaction(channelId, resultMessage.tid, key)

            if (emitUpdate) {
                val message =
                    messageDao.getMessageById(messageId)?.toSceytMessage() ?: resultMessage

                messagesCache.messageUpdated(channelId, message)
                ChatReactionMessagesCache.addMessage(message)

                if (!message.incoming) {
                    val reaction = message.userReactions?.maxByOrNull { it.createdAt }
                    if (reaction != null)
                        handleChannelReaction(
                            data = ReactionUpdateEventData(message, reaction, Add),
                            message = message
                        )
                }
            }
        }.onError {
            val errorType = SDKErrorTypeEnum.fromValue(it?.type) ?: return@onError
            if (!errorType.isResendable) {
                SceytLog.e(
                    TAG,
                    "Delete reaction error: ${it?.message}, errorType: ${errorType.value}" +
                            "channel id $channelId, reaction:${key} will be deleted due to non-resendable error"
                )
                pendingReactionDao.deletePendingReaction(messageId, key)
                messagesCache.deletePendingReaction(channelId, messageId, key)
            }
        }
        return response
    }

    private suspend fun insertPendingReactionToDbAndGetWasPending(
        channelId: Long,
        messageId: Long,
        key: String,
        reason: String,
        enforceUnique: Boolean,
        isAdd: Boolean
    ): Boolean {
        val messageDb = messageDao.getMessageById(messageId) ?: return false
        val pendingReactions = messageDb.pendingReactions?.toArrayList() ?: arrayListOf()
        var wasPending = false
        var pendingReactionEntity = pendingReactions.find { it.key == key }
        if (pendingReactionEntity != null) {
            // if pending reaction already exists, and isAdd is different, remove it.
            if (pendingReactionEntity.isAdd != isAdd) {
                pendingReactions.remove(pendingReactionEntity)
                pendingReactionDao.deletePendingReaction(messageId, key)
                messagesCache.deletePendingReaction(channelId, messageDb.messageEntity.tid, key)
                pendingReactionEntity = null
                wasPending = true
            }
        } else {
            val entity = PendingReactionEntity(
                messageId = messageId,
                channelId = channelId,
                key = key,
                score = 1,
                reason = reason,
                enforceUnique = enforceUnique,
                createdAt = System.currentTimeMillis(),
                isAdd = isAdd,
                incomingMsg = messageDb.messageEntity.incoming,
                count = 1
            )
            pendingReactions.add(entity)
            pendingReactionEntity = entity
        }

        val message = messageDb.copy(pendingReactions = pendingReactions).toSceytMessage()
        messagesCache.messageUpdated(channelId, message)
        ChatReactionMessagesCache.addMessage(message)

        pendingReactionEntity?.let { pendingReactionDao.insertIfMessageExist(it) }

        if (!message.incoming)
            notifyChannelReactionUpdated(channelId)

        return wasPending
    }

    private suspend fun notifyChannelReactionUpdated(channelId: Long) {
        channelDao.getChannelById(channelId)?.toChannel()?.let {
            channelsCache.upsertChannel(it)
        }
    }

    private suspend fun getReactionsDb(
        messageId: Long,
        offset: Int,
        limit: Int,
        key: String
    ): List<SceytReaction> {
        return if (key.isBlank()) {
            reactionDao.getReactions(
                messageId = messageId,
                limit = limit,
                offset = offset
            ).map { it.toSceytReaction() }
        } else {
            reactionDao.getReactionsByKey(
                messageId = messageId,
                limit = limit,
                offset = offset,
                key = key
            ).map { it.toSceytReaction() }
        }
    }

    private suspend fun saveReactionsToDb(list: List<SceytReaction>) {
        if (list.isEmpty()) return
        reactionDao.insertReactionsIfMessageExist(list.map { it.toReactionEntity() })
        usersDao.insertUsersWithMetadata(list.mapNotNull { it.user?.toUserDb() })
    }

    companion object {
        private const val TAG = "PersistenceReactionsLogic"
    }
}