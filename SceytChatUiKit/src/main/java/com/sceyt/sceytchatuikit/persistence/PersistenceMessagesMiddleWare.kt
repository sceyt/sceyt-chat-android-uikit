package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface PersistenceMessagesMiddleWare {
    suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long,
                                 replyInThread: Boolean, offset: Int, limit: Int = SceytKitConfig.MESSAGES_LOAD_SIZE,
                                 loadKey: LoadKeyData, ignoreDb: Boolean = false): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                 offset: Int, limit: Int = SceytKitConfig.MESSAGES_LOAD_SIZE,
                                 ignoreDb: Boolean = false): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                 limit: Int, loadKey: LoadKeyData,
                                 ignoreDb: Boolean = false, ignoreServer: Boolean = false): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, limit: Int = SceytKitConfig.MESSAGES_LOAD_SIZE,
                                   loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>>

    suspend fun syncMessagesAfterMessageId(conversationId: Long, replyInThread: Boolean,
                                           messageId: Long): Flow<SceytResponse<List<SceytMessage>>>

    suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult>
    suspend fun sendMessage(channelId: Long, message: Message)
    suspend fun sendMessages(channelId: Long, messages: List<Message>)
    suspend fun sendSharedFileMessage(channelId: Long, message: Message)
    suspend fun sendFrowardMessages(channelId: Long, messagesToSend: List<Message>): SceytResponse<Boolean>
    suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message): SceytResponse<SceytMessage>
    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun sendAllPendingMarkers()
    suspend fun sendAllPendingReactions()
    suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>>
    suspend fun markMessagesAsDelivered(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>>
    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun getMessageFromServerById(channelId: Long, messageId: Long): SceytResponse<SceytMessage>
    suspend fun getMessageDbById(messageId: Long): SceytMessage?
    suspend fun getMessageDbByTid(messageTid: Long): SceytMessage?
    fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>>
}