package com.sceyt.sceytchatuikit.persistence.logics.reactionslogic

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
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
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
        private var channelsCache: ChannelsCache,
        private var messagesCache: MessagesCache) : PersistenceReactionsLogic {

    override suspend fun onMessageReactionUpdated(data: ReactionUpdateEventData) {
        val messageId = data.message.id
        reactionDao.deleteAllReactionScoresByMessageId(messageId)
        val existMessage = messageDao.existsMessageById(messageId)
        if (!existMessage)
            messageDao.upsertMessage(data.message.toMessageDb(false))

        when (data.eventType) {
            ADD -> reactionDao.insertReaction(data.reaction.toReactionEntity())
            REMOVE -> reactionDao.deleteReaction(messageId, data.reaction.key, data.reaction.user.id)
        }
        data.message.reactionTotals?.map { it.toReactionTotalEntity(messageId) }?.let {
            reactionDao.insertReactionScores(it)
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

    override suspend fun addReaction(channelId: Long, messageId: Long, key: String): SceytResponse<SceytMessage> {
        val response = reactionsRepository.addReaction(channelId, messageId, key)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                resultMessage.userReactions?.let {
                    messageDao.insertReactions(it.map { reaction -> reaction.toReactionEntity() })
                }
                resultMessage.reactionTotals?.let {
                    messageDao.insertReactionScores(it.map { score -> score.toReactionTotalEntity(messageId) })
                }

                val message = messageDao.getMessageById(messageId)?.toSceytMessage()
                        ?: resultMessage

                messagesCache.messageUpdated(channelId, message)

                if (!message.incoming) {
                    val reaction = message.userReactions?.maxBy { it.id }
                    if (reaction != null)
                        handleChannelReaction(ReactionUpdateEventData(message, reaction, ADD), message)
                }
            }
        }
        return response
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, key: String): SceytResponse<SceytMessage> {
        val response = reactionsRepository.deleteReaction(channelId, messageId, key)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                SceytKitClient.myId?.let { fromId ->
                    reactionDao.deleteReactionAndScore(messageId, key, fromId)
                }

                val message = messageDao.getMessageById(messageId)?.toSceytMessage()
                        ?: resultMessage

                messagesCache.messageUpdated(channelId, message)

                if (!message.incoming) {
                    val reaction = Reaction(0, messageId, key, 1, "", 0, ClientWrapper.currentUser)
                    handleChannelReaction(ReactionUpdateEventData(message, reaction, REMOVE), message)
                }
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

    private suspend fun saveReactionsToDb(list: List<Reaction>) {
        if (list.isEmpty()) return

        reactionDao.insertReactions(list.map { it.toReactionEntity() })
        usersDao.insertUsers(list.mapNotNull { it.user?.toUserEntity() })
    }
}