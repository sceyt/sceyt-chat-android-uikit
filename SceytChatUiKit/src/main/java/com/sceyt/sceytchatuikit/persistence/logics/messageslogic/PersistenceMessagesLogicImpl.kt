package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.LoadNearData
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.mappers.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.LoadKeyType
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion

internal class PersistenceMessagesLogicImpl(
        private val messageDao: MessageDao,
        private val reactionDao: ReactionDao,
        private val persistenceChannelsLogic: PersistenceChannelsLogic,
        private val userDao: UserDao,
        private val messagesRepository: MessagesRepository,
        private val preference: SceytSharedPreference,
        private val messagesCash: MessagesCash
) : PersistenceMessagesLogic, SceytKoinComponent {

    override suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        val message = data.second
        messageDao.insertMessage(message.toMessageDb())
        messagesCash.add(message)
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        messageDao.updateMessageStatus(data.status, *data.messageIds.toLongArray())
        messagesCash.messageUpdated(*messageDao.getMessageByIds(data.messageIds).map { it.toSceytMessage() }.toTypedArray())
    }

    override suspend fun onMessageReactionUpdated(data: Message?) {
        data ?: return
        reactionDao.insertReactionsAndScores(
            messageId = data.id,
            reactionsDb = data.lastReactions.map { it.toReactionEntity(data.id) },
            scoresDb = data.reactionScores.map { it.toReactionScoreEntity(data.id) })
        messagesCash.messageUpdated(data.toSceytUiMessage())
    }

    override suspend fun onMessageEditedOrDeleted(data: Message?) {
        data ?: return
        messageDao.updateMessage(data.toMessageEntity())
        messagesCash.messageUpdated(data.toSceytUiMessage())
    }

    override suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean,
                                          offset: Int, loadKey: Long, ignoreDb: Boolean, ignoreCash: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadPrev, conversationId, lastMessageId, replayInThread, offset, loadKey, ignoreDb, ignoreCash)
    }

    override suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean,
                                          offset: Int, ignoreDb: Boolean, ignoreCash: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNext, conversationId, lastMessageId, replayInThread, offset, ignoreDb = ignoreDb, ignoreCash = ignoreCash)
    }

    override suspend fun loadNearMessages(conversationId: Long, messageId: Long, replayInThread: Boolean,
                                          loadKey: Long, ignoreDb: Boolean, ignoreCash: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNear, conversationId, messageId, replayInThread, 0, loadKey, ignoreDb, ignoreCash)
    }

    override suspend fun loadNewestMessages(conversationId: Long, replayInThread: Boolean, loadKey: Long,
                                            ignoreDb: Boolean,
                                            ignoreCash: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNewest, conversationId, 0, replayInThread, 0, loadKey, ignoreDb, ignoreCash)
    }

    override suspend fun syncMessagesAfterMessageId(conversationId: Long, replayInThread: Boolean,
                                                    messageId: Long): Flow<SceytResponse<List<SceytMessage>>> = callbackFlow {
        ConnectionEventsObserver.awaitToConnectSceyt()
        messagesRepository.loadAllMessagesAfter(conversationId, replayInThread, messageId)
            .onCompletion { channel.close() }
            .collect {
                if (it is SceytResponse.Success) {
                    it.data?.let { messages ->
                        saveMessagesToDb(messages)
                        markMessagesAsReceived(conversationId, messages)
                    }
                }
                trySend(it)
            }
        awaitClose()
    }

    private fun loadMessages(loadType: LoadType, conversationId: Long, messageId: Long,
                             replayInThread: Boolean, offset: Int, loadKey: Long = messageId,
                             ignoreDb: Boolean, ignoreCash: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return callbackFlow {
            if (offset == 0 && !ignoreCash) messagesCash.clear()

            // Load from database
            if (!ignoreDb)
                trySend(getMessagesDbByLoadType(loadType, conversationId, messageId, offset, loadKey, ignoreCash))
            // Load from server
            trySend(getMessagesServerByLoadType(loadType, conversationId, messageId, offset, replayInThread,
                loadKey, ignoreDb))

            channel.close()
            awaitClose()
        }
    }

    private suspend fun getMessagesDbByLoadType(loadType: LoadType, channelId: Long, lastMessageId: Long,
                                                offset: Int, loadKey: Long, ignoreCash: Boolean): PaginationResponse.DBResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        val messages: List<SceytMessage>

        when (loadType) {
            LoadPrev -> {
                messages = getPrevMessagesDb(channelId, lastMessageId, offset)
                hasPrev = messages.size == MESSAGES_LOAD_SIZE
            }
            LoadNext -> {
                messages = getNextMessagesDb(channelId, lastMessageId, offset)
                hasNext = messages.size == MESSAGES_LOAD_SIZE
            }
            LoadNear -> {
                val data = getNearMessagesDb(channelId, lastMessageId, offset)
                messages = data.data.map { it.toSceytMessage() }
                hasPrev = data.hasPrev
                hasNext = data.hasNext
            }
            LoadNewest -> {
                messages = getPrevMessagesDb(channelId, Long.MAX_VALUE, offset)
                hasPrev = messages.size == MESSAGES_LOAD_SIZE
            }
        }

        if (!ignoreCash)
            messagesCash.addAll(messages, false)

        // Mark messages as received
        markMessagesAsReceived(channelId, messages)

        return PaginationResponse.DBResponse(messages, loadKey, offset, hasNext, hasPrev, loadType)
    }

    private suspend fun getMessagesServerByLoadType(loadType: LoadType, channelId: Long, lastMessageId: Long,
                                                    offset: Int, replayInThread: Boolean, loadKey: Long = lastMessageId,
                                                    ignoreDb: Boolean): PaginationResponse.ServerResponse2<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        val hasDiff: Boolean
        var messages: List<SceytMessage> = emptyList()
        val response: SceytResponse<List<SceytMessage>>

        when (loadType) {
            LoadPrev -> {
                response = messagesRepository.getPrevMessages(channelId, lastMessageId, replayInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == MESSAGES_LOAD_SIZE
                }
            }
            LoadNext -> {
                response = messagesRepository.getNextMessages(channelId, lastMessageId, replayInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasNext = response.data?.size == MESSAGES_LOAD_SIZE
                }
            }
            LoadNear -> {
                response = messagesRepository.getNearMessages(channelId, lastMessageId, replayInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    val groupOldAndNewData = messages.groupBy { it.id > lastMessageId }

                    val newest = groupOldAndNewData[true]
                    val oldest = groupOldAndNewData[false]

                    hasNext = (newest?.size ?: 0) >= MESSAGES_LOAD_SIZE / 2
                    hasPrev = (oldest?.size ?: 0) >= MESSAGES_LOAD_SIZE / 2
                }
            }
            LoadNewest -> {
                response = messagesRepository.getPrevMessages(channelId, Long.MAX_VALUE, replayInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == MESSAGES_LOAD_SIZE
                }
            }
        }

        saveMessagesToDb(messages)

        if (loadType == LoadNear && loadKey == LoadKeyType.ScrollToMessageById.longValue)
            messagesCash.clear()

        hasDiff = messagesCash.addAll(messages, true)

        // Mark messages as received
        markMessagesAsReceived(channelId, messages)

        return PaginationResponse.ServerResponse2(
            data = response, cashData = messagesCash.getSorted(),
            loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext,
            hasPrev = hasPrev, loadType = loadType, ignoredDb = ignoreDb)
    }

    private suspend fun getPrevMessagesDb(channelId: Long, lastMessageId: Long, offset: Int): List<SceytMessage> {
        var lastMsgId = lastMessageId
        if (lastMessageId == 0L)
            lastMsgId = Long.MAX_VALUE

        var messages = messageDao.getOldestThenMessages(channelId, lastMsgId, MESSAGES_LOAD_SIZE).reversed()

        if (offset == 0)
            messages = getPendingMessagesAndAddTList(channelId, messages.toArrayList())

        return messages.map { messageDb -> messageDb.toSceytMessage() }
    }

    private suspend fun getNextMessagesDb(channelId: Long, lastMessageId: Long, offset: Int): List<SceytMessage> {
        var messages = messageDao.getNewestThenMessage(channelId, lastMessageId, MESSAGES_LOAD_SIZE)

        if (offset == 0)
            messages = getPendingMessagesAndAddTList(channelId, messages.toArrayList())

        return messages.map { messageDb -> messageDb.toSceytMessage() }
    }

    private suspend fun getNearMessagesDb(channelId: Long, messageId: Long, offset: Int): LoadNearData<MessageDb> {
        val data = messageDao.getNearMessages(channelId, messageId, MESSAGES_LOAD_SIZE)
        val messages = data.data

        if (offset == 0)
            data.data = getPendingMessagesAndAddTList(channelId, messages.toArrayList())

        return data
    }

    private suspend fun getPendingMessagesAndAddTList(channelId: Long, list: ArrayList<MessageDb>): ArrayList<MessageDb> {
        val pendingMessage = messageDao.getPendingMessages(channelId)

        if (pendingMessage.isNotEmpty())
            list.addAll(pendingMessage)
        return list
    }

    private suspend fun markMessagesAsReceived(channelId: Long, messages: List<SceytMessage>) {
        val notDisplayedMessages = messages.filter {
            it.incoming && it.selfMarkers?.contains(SelfMarkerTypeEnum.Received.toString()) != true
        }
        if (notDisplayedMessages.isNotEmpty())
            markMessagesAsReceive(channelId, *notDisplayedMessages.map { it.id }.toLongArray())
    }

    private suspend fun saveMessagesToDb(list: List<SceytMessage>) {
        if (list.isEmpty()) return

        messageDao.insertMessages(list.map {
            it.toMessageDb()
        })

        // Update users
        val usersDb = arrayListOf<UserEntity>()
        list.filter { it.incoming && it.from != null }.map { it.from!! }.toSet().let { users ->
            if (users.isNotEmpty())
                usersDb.addAll(users.map { it.toUserEntity() })
        }

        // Users which added reaction
        val usersByReaction = list.flatMap {
            it.lastReactions?.toMutableList() ?: mutableListOf()
        }.map {
            it.user.toUserEntity()
        }
        usersDb.addAll(usersByReaction)

        userDao.insertUsers(usersDb)
    }

    override suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?> {
        val response = messagesRepository.sendMessage(channelId, message) { tmpMessage ->
            val tmpMessageDb = tmpMessage.toMessageDb().also {
                it.messageEntity.id = null
                if (tmpMessage.replyInThread)
                    it.messageEntity.channelId = tmpMessage.parent.id
            }
            messageDao.insertMessage(tmpMessageDb)
            persistenceChannelsLogic.updateLastMessageWithLastRead(channelId, tmpMessage.toSceytUiMessage())
            messagesCash.add(tmpMessage.toSceytUiMessage())
            tmpMessageCb.invoke(tmpMessage)
            MessageEventsObserver.emitOutgoingMessage(tmpMessage.toSceytUiMessage())
        }
        if (response is SceytResponse.Success) {
            response.data?.let { responseMsg ->
                messageDao.updateMessageByParams(
                    tid = responseMsg.tid, serverId = responseMsg.id,
                    date = responseMsg.createdAt, status = DeliveryStatus.Sent)

                messagesCash.messageUpdated(responseMsg)
                persistenceChannelsLogic.updateLastMessageWithLastRead(channelId, responseMsg)
                MessageEventsObserver.emitOutgoingMessageSent(channelId, response.data)
            }
        }
        return response
    }

    override suspend fun sendPendingMessages(channelId: Long) {
        val pendingMessages = messageDao.getPendingMessages(channelId)
        if (pendingMessages.isNotEmpty()) {
            pendingMessages.forEach {
                val response = messagesRepository.sendMessage(channelId, it.toMessage())
                if (response is SceytResponse.Success) {
                    response.data?.let { responseMsg ->
                        messageDao.updateMessageByParams(
                            tid = responseMsg.tid, serverId = responseMsg.id,
                            date = responseMsg.createdAt, status = DeliveryStatus.Sent)

                        messagesCash.messageUpdated(responseMsg)
                        persistenceChannelsLogic.updateLastMessageWithLastRead(channelId, responseMsg)
                        MessageEventsObserver.emitOutgoingMessageSent(channelId, response.data)
                    }
                }
            }
        }
    }

    override suspend fun sendAllPendingMessages() {
        val pendingMessages = messageDao.getAllPendingMessages()
        if (pendingMessages.isNotEmpty()) {
            pendingMessages.forEach {
                val response = messagesRepository.sendMessage(it.messageEntity.channelId, it.toMessage())
                if (response is SceytResponse.Success) {
                    response.data?.let { responseMsg ->
                        messageDao.updateMessageByParams(
                            tid = responseMsg.tid, serverId = responseMsg.id,
                            date = responseMsg.createdAt, status = DeliveryStatus.Sent)

                        messagesCash.messageUpdated(responseMsg)
                        MessageEventsObserver.emitOutgoingMessageSent(it.messageEntity.channelId, response.data)
                    }
                }
            }
        }
    }

    override suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            messageDao.deleteMessageByTid(message.tid)
            messagesCash.deleteMessage(message.tid)
            return SceytResponse.Success(message.apply { state = MessageState.Deleted })
        }
        val response = messagesRepository.deleteMessage(channelId, message.id, onlyForMe)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                messageDao.deleteAttachments(listOf(message.tid))
                reactionDao.deleteAllReactionsAndScores(message.id)
                messageDao.updateMessage(resultMessage.toMessageEntity())
                messagesCash.messageUpdated(resultMessage)
            }
        }
        return response
    }

    override suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        val response = messagesRepository.markAsRead(channelId, *ids)
        if (response is SceytResponse.Success) {
            response.data?.let { messageListMarker ->
                messageDao.updateMessagesStatus(channelId, messageListMarker.messageIds, DeliveryStatus.Read)
                val tIds = messageDao.getMessageTIdsByIds(*ids)
                messagesCash.updateMessagesStatus(DeliveryStatus.Read, *tIds.toLongArray())

                //todo need update marker count
                ids.forEach {
                    messageDao.updateMessageSelfMarkersAndMarkerCount(channelId, it, SelfMarkerTypeEnum.Displayed.toString())
                }
            }
        }
        return response
    }

    override suspend fun markMessagesAsReceive(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        val response = messagesRepository.markAsDelivered(channelId, *ids)
        if (response is SceytResponse.Success) {
            response.data?.let { messageListMarker ->
                messageDao.updateMessagesStatus(channelId, messageListMarker.messageIds, DeliveryStatus.Delivered)
                val tIds = messageDao.getMessageTIdsByIds(*ids)
                messagesCash.updateMessagesStatus(DeliveryStatus.Delivered, *tIds.toLongArray())

                //todo need update marker count
                ids.forEach {
                    messageDao.updateMessageSelfMarkersAndMarkerCount(channelId, it, SelfMarkerTypeEnum.Received.toString())
                }
            }
        }
        return response
    }

    override suspend fun editMessage(id: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        val response = messagesRepository.editMessage(id, message)
        if (response is SceytResponse.Success) {
            response.data?.let { updatedMsg ->
                messageDao.updateMessage(updatedMsg.toMessageEntity())
                messagesCash.messageUpdated(updatedMsg)
            }
        }
        return response
    }

    override suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        val response = messagesRepository.addReaction(channelId, messageId, scoreKey)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                message.lastReactions?.let {
                    messageDao.insertReactions(it.map { reaction -> reaction.toReactionEntity(messageId) })
                }
                message.reactionScores?.let {
                    messageDao.insertReactionScores(it.map { score -> score.toReactionScoreEntity(messageId) })
                }
                messagesCash.messageUpdated(message)
            }
        }
        return response
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        val response = messagesRepository.deleteReaction(channelId, messageId, scoreKey)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                message.reactionScores?.let {
                    val fromId = preference.getUserId()
                    if (fromId != null)
                        reactionDao.deleteReactionAndScore(messageId, scoreKey, fromId)
                }
                messagesCash.messageUpdated(message)
            }
        }
        return response
    }
}