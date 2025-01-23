package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SendMessageResult
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.push.PushData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface PersistenceMessagesLogic {
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>, sendDeliveryMarker: Boolean = true)
    suspend fun handlePush(data: PushData): Boolean
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    suspend fun onMessageEditedOrDeleted(message: SceytMessage)
    suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
                                 limit: Int, loadKey: LoadKeyData,
                                 ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                 offset: Int, limit: Int, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                 limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean,
                                 ignoreServer: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, limit: Int,
                                   loadKey: LoadKeyData,
                                   ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun searchMessages(conversationId: Long, replyInThread: Boolean,
                               query: String): SceytPagingResponse<List<SceytMessage>>

    suspend fun loadNextSearchMessages(): SceytPagingResponse<List<SceytMessage>>
    suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>>
    suspend fun syncMessagesAfterMessageId(conversationId: Long, replyInThread: Boolean,
                                           messageId: Long): Flow<SceytResponse<List<SceytMessage>>>

    suspend fun syncNearMessages(conversationId: Long, messageId: Long,
                                 replyInThread: Boolean): SyncNearMessagesResult

    suspend fun onSyncedChannels(channels: List<SceytChannel>)
    suspend fun getMessagesByType(channelId: Long, lastMessageId: Long,
                                  type: String): SceytResponse<List<SceytMessage>>

    suspend fun sendMessage(channelId: Long, message: Message)
    suspend fun sendMessages(channelId: Long, messages: List<Message>)
    suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult>
    suspend fun sendSharedFileMessage(channelId: Long, message: Message)
    suspend fun sendFrowardMessages(channelId: Long, vararg messageToSend: Message): SceytResponse<Boolean>
    suspend fun sendMessageWithUploadedAttachments(channelId: Long,
                                                   message: Message): SceytResponse<SceytMessage>

    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun sendAllPendingMarkers()
    suspend fun sendAllPendingMessageStateUpdates()
    suspend fun markMessagesAs(channelId: Long, marker: MarkerType,
                               vararg ids: Long): List<SceytResponse<MessageListMarker>>

    suspend fun addMessagesMarker(channelId: Long, marker: String,
                                  vararg ids: Long): List<SceytResponse<MessageListMarker>>

    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun deleteMessage(channelId: Long, message: SceytMessage,
                              deleteType: DeleteMessageType): SceytResponse<SceytMessage>

    suspend fun getMessageDbById(messageId: Long): SceytMessage?
    suspend fun getMessageDbByTid(tid: Long): SceytMessage?
    suspend fun getMessagesDbByTid(tIds: List<Long>): List<SceytMessage>
    suspend fun getMessageFromServerById(channelId: Long, messageId: Long): SceytResponse<SceytMessage>
    suspend fun attachmentSuccessfullySent(message: SceytMessage)
    suspend fun saveChannelLastMessagesToDb(list: List<SceytMessage>?)
    suspend fun sendTyping(channelId: Long, typing: Boolean)
    fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>>
}