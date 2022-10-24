package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.data.toSceytUiMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.mappers.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class PersistenceMessagesLogicImpl(
        private val messageDao: MessageDao,
        private val reactionDao: ReactionDao,
        private val channelDao: ChannelDao,
        private val userDao: UserDao,
        private val messagesRepository: MessagesRepository,
        private val preference: SceytSharedPreference
) : PersistenceMessagesLogic, SceytKoinComponent {

    override suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        val message = data.second
        //Message tid is message id
        message.tid = message.id
        messageDao.insertMessage(message.toMessageDb())
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        messageDao.updateMessageStatus(data.status, *data.messageIds.toLongArray())
    }

    override fun onMessageReactionUpdated(data: Message?) {
        data ?: return
        reactionDao.insertReactionsAndScores(
            reactionsDb = data.lastReactions.map { it.toReactionEntity(data.id) },
            scoresDb = data.reactionScores.map { it.toReactionScoreEntity(data.id) })
    }

    override fun onMessageEditedOrDeleted(data: Message?) {
        data ?: return
        messageDao.updateMessageStateAndBody(data.id, data.state, data.body)
    }

    override fun loadMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>> {
        return callbackFlow {
            val dbMessages = getMessagesDb(conversationId, lastMessageId, offset)
            trySend(PaginationResponse.DBResponse(dbMessages, offset, hasNext = dbMessages.size == MESSAGES_LOAD_SIZE))

            val response = messagesRepository.getMessages(conversationId, lastMessageId, replayInThread)

            val hasNext = response is SceytResponse.Success && response.data?.size == MESSAGES_LOAD_SIZE
            trySend(PaginationResponse.ServerResponse(data = response, offset = offset, dbData = arrayListOf(), hasNext = hasNext))

            if (response is SceytResponse.Success)
                saveMessagesToDb(response.data ?: return@callbackFlow)

            awaitClose()
        }
    }

    private suspend fun getMessagesDb(channelId: Long, lastMessageId: Long, offset: Int): List<SceytMessage> {
        var lastMsgId = lastMessageId
        if (lastMessageId == 0L)
            lastMsgId = Long.MAX_VALUE


        var messages = messageDao.getMessages(channelId, lastMsgId, MESSAGES_LOAD_SIZE)
            .map { messageDb -> messageDb.toSceytMessage() }.reversed()

        if (offset == 0) {
            val pendingMessage = messageDao.getPendingMessages(channelId)
                .map { messageDb -> messageDb.toSceytMessage() }

            if (pendingMessage.isNotEmpty())
                messages = ArrayList(messages).apply { addAll(pendingMessage) }
        }
        return messages
    }

    private fun saveMessagesToDb(list: List<SceytMessage>) {
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
            channelDao.updateLastMessage(channelId, tmpMessage.tid, tmpMessage.createdAt.time)
            tmpMessageCb.invoke(tmpMessage)
            MessageEventsObserver.emitOutgoingMessage(tmpMessage.toSceytUiMessage())
        }
        if (response is SceytResponse.Success) {
            response.data?.let { responseMsg ->
                messageDao.updateMessageByParams(
                    tid = responseMsg.tid, serverId = responseMsg.id,
                    date = responseMsg.createdAt, status = DeliveryStatus.Sent)

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

                        MessageEventsObserver.emitOutgoingMessageSent(channelId, response.data)
                    }
                }
            }
        }
    }

    override suspend fun deleteMessage(channelId: Long, messageId: Long, messageTid: Long): SceytResponse<SceytMessage> {
        val response = messagesRepository.deleteMessage(channelId, messageId)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                messageDao.deleteAttachments(listOf(messageTid))
                reactionDao.deleteAllReactionsAndScores(messageId)
                messageDao.updateMessage(message.toMessageEntity())
            }
        }
        return response
    }

    override suspend fun markAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        val response = messagesRepository.markAsRead(channelId, *ids)
        if (response is SceytResponse.Success) {
            response.data?.let { messageListMarker ->
                messageDao.updateMessagesStatusAsRead(channelId, messageListMarker.messageIds)
                channelDao.clearUnreadCount(channelId, 0)
            }
        }
        return response
    }

    override suspend fun editMessage(id: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        val response = messagesRepository.editMessage(id, message)
        if (response is SceytResponse.Success) {
            response.data?.let { updatedMsg ->
                messageDao.updateMessage(updatedMsg.toMessageEntity())
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
            }
        }
        return response
    }
}