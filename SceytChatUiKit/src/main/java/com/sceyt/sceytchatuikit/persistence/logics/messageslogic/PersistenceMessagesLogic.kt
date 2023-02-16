package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

internal interface PersistenceMessagesLogic {
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>, sendDeliveryMarker: Boolean = true)
    fun onFcmMessage(data: Pair<SceytChannel, SceytMessage>)
    suspend fun onChannelEvent(data: ChannelEventData)
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    suspend fun onMessageReactionUpdated(data: Message)
    suspend fun onMessageEditedOrDeleted(data: SceytMessage)
    suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
                                 loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
                                 ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                 loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, loadKey: LoadKeyData,
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
    suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message): SceytResponse<SceytMessage>
    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun sendAllPendingMarkers()
    suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun markMessageAsDelivered(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker>
    suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker>
    suspend fun editMessage(id: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun getMessageFromDbById(messageId: Long): SceytMessage?
    suspend fun getMessageFromDbByTid(tid: Long): SceytMessage?
    suspend fun attachmentSuccessfullySent(message: SceytMessage)
    fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>>
}