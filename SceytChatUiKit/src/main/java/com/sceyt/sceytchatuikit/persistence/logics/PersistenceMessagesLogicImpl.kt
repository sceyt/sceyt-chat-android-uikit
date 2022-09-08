package com.sceyt.sceytchatuikit.persistence.logics

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.SceytKoinComponent
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.mappers.toMessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class PersistenceMessagesLogicImpl(
        private val messageDao: MessageDao,
        private val channelDao: ChannelDao,
        private val userDao: UserDao,
        private val messagesRepository: MessagesRepository
) : PersistenceMessagesLogic, SceytKoinComponent {

    override suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        val message = data.second
        message.tid = message.id
        messageDao.insertMessage(message.toMessageDb())
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        messageDao.updateMessageStatus(data.status, *data.messageIds.toLongArray())
    }

    override fun onMessageReactionUpdated(data: Message?) {
        data ?: return
        //TODO not yet implemented
    }

    override fun onMessageEditedOrDeleted(data: Message?) {
        data ?: return
        messageDao.updateMessageStateAndBody(data.id, data.state, data.body)
    }

    override fun loadMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>> {
        return callbackFlow {
            val dbMessages = getMessagesDb(conversationId, lastMessageId, offset)
            trySend(PaginationResponse.DBResponse(dbMessages, offset))

            val response = messagesRepository.getMessages(conversationId, lastMessageId, replayInThread)

            trySend(PaginationResponse.ServerResponse(data = response, offset = offset, dbData = arrayListOf()))

            if (response is SceytResponse.Success)
                saveMessagesToDb(response.data ?: return@callbackFlow)

            awaitClose()
        }
    }

    private suspend fun getMessagesDb(channelId: Long, lastMessageId: Long, offset: Int): List<SceytMessage> {
        var lastMsgId = lastMessageId
        if (lastMessageId == 0L)
            lastMsgId = Long.MAX_VALUE


        val messages = messageDao.getMessages(channelId, lastMsgId, SceytUIKitConfig.MESSAGES_LOAD_SIZE)
            .map { messageDb -> messageDb.toSceytMessage() }.reversed()

        if (offset == 0) {
            val pendingMessage = messageDao.getPendingMessages(channelId)
                .map { messageDb -> messageDb.toSceytMessage() }
            if (pendingMessage.isNotEmpty())
                (messages as ArrayList).addAll(pendingMessage)
        }
        return messages
    }

    private fun saveMessagesToDb(list: List<SceytMessage>) {
        if (list.isEmpty()) return

        messageDao.insertMessages(list.map {
            it.tid = it.id
            it.toMessageDb()
        })

        // Update users
        list.filter { it.incoming && it.from != null }.map { it.from!! }.toSet().let { users ->
            if (users.isNotEmpty())
                userDao.insertUsers(users.map { it.toUserEntity() })
        }
    }

    override suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?> {
        val response = messagesRepository.sendMessage(channelId, message) { tmpMessage ->
            val tmpMessageDb = tmpMessage.toMessageDb().also { it.messageEntity.id = null }
            messageDao.insertMessage(tmpMessageDb)
            channelDao.updateLastMessage(channelId, tmpMessage.tid, tmpMessage.createdAt.time)
            tmpMessageCb.invoke(tmpMessage)
        }
        if (response is SceytResponse.Success) {
            response.data?.let { responseMsg ->
                messageDao.updateMessageByParams(
                    tid = responseMsg.tid, serverId = responseMsg.id,
                    date = responseMsg.createdAt, status = DeliveryStatus.Sent)
                channelDao.updateLastMessage(channelId, responseMsg.id, responseMsg.createdAt)
            }
        }
        return response
    }

    override suspend fun deleteMessage(channelId: Long, messageId: Long): SceytResponse<SceytMessage> {
        val response = messagesRepository.deleteMessage(channelId, messageId)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                messageDao.deleteAttachments(messageId)
                messageDao.insertMessage(message.toMessageDb())
            }
        }
        return response
    }

    override suspend fun markAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        val response = messagesRepository.markAsRead(channelId, *ids)
        if (response is SceytResponse.Success) {
            response.data?.let { messageListMarker ->
                messageListMarker.messageIds
                messageDao.updateMessagesStatusAsRead(channelId, messageListMarker.messageIds)
            }
        }
        return response
    }
}