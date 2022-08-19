package com.sceyt.chat.ui.persistence.logics

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.repositories.MessagesRepository
import com.sceyt.chat.ui.data.toChannel
import com.sceyt.chat.ui.persistence.dao.MessageDao
import com.sceyt.chat.ui.persistence.mappers.toMessageEntity
import com.sceyt.chat.ui.persistence.mappers.toSceytMessage
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.component.KoinComponent

internal class PersistenceMessagesLogicImpl(
        private val messageDao: MessageDao,
        private val messagesRepository: MessagesRepository
) : PersistenceMessagesLogic, KoinComponent {

    override fun onMessage(data: Pair<Channel, Message>) {
        messageDao.insertMessage(data.second.toMessageEntity())
    }

    override fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
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

    override fun loadMessages(channel: SceytChannel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return callbackFlow {
            val dbMessages = getMessagesDb(channel.id, lastMessageId)
            //Todo offset
            trySend(PaginationResponse.DBResponse(dbMessages, 0))

            val response = messagesRepository.getMessages(channel.toChannel(), conversationId, lastMessageId, replayInThread)

            //Todo offset
            trySend(PaginationResponse.ServerResponse(data = response, offset = 0, dbData = arrayListOf()))

            if (response is SceytResponse.Success) {
                saveMessagesToDb(response.data ?: return@callbackFlow)
            }
            awaitClose()
        }
    }

    private fun getMessagesDb(channelId: Long, lastMessageId: Long): List<SceytMessage> {
        var lastMsgId = lastMessageId
        if (lastMessageId == 0L)
            lastMsgId = Long.MAX_VALUE
        return messageDao.getMessages(channelId, lastMsgId, SceytUIKitConfig.MESSAGES_LOAD_SIZE)
            .map { messageDb -> messageDb.toSceytMessage() }
    }

    private fun saveMessagesToDb(list: List<SceytMessage>) {
        if (list.isEmpty()) return

        messageDao.insertMessages(list.map { it.toMessageEntity() })
    }
}