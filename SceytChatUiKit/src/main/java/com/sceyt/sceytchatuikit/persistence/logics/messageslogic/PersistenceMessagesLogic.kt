package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.pushes.RemoteMessageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface PersistenceMessagesLogic {
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>, sendDeliveryMarker: Boolean = true)
    suspend fun onFcmMessage(data: RemoteMessageData)
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    suspend fun onMessageEditedOrDeleted(data: SceytMessage)
    suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
                                 limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
                                 limit: Int,
                                 ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                 limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean, ignoreServer: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, limit: Int,
                                   loadKey: LoadKeyData,
                                   ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>>

    suspend fun syncMessagesAfterMessageId(conversationId: Long, replyInThread: Boolean,
                                           messageId: Long): Flow<SceytResponse<List<SceytMessage>>>

    suspend fun onSyncedChannels(channels: List<SceytChannel>)
    suspend fun getMessagesByType(channelId: Long, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>>
    suspend fun sendMessage(channelId: Long, message: Message)
    suspend fun sendMessages(channelId: Long, messages: List<Message>)
    suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult>
    suspend fun sendSharedFileMessage(channelId: Long, message: Message)
    suspend fun sendFrowardMessages(channelId: Long, vararg messageToSend: Message): SceytResponse<Boolean>
    suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message): SceytResponse<SceytMessage>
    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun sendAllPendingMarkers()
    suspend fun sendAllPendingMessageStateUpdates()
    suspend fun markMessageAsDelivered(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>>
    suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>>
    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun getMessageDbById(messageId: Long): SceytMessage?
    suspend fun getMessageDbByTid(tid: Long): SceytMessage?
    suspend fun getMessagesDbByTid(tIds: List<Long>): List<SceytMessage>
    suspend fun getMessageFromServerById(channelId: Long, messageId: Long): SceytResponse<SceytMessage>
    suspend fun attachmentSuccessfullySent(message: SceytMessage)
    suspend fun saveChannelLastMessagesToDb(list: List<SceytMessage>?)
    fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>>
}