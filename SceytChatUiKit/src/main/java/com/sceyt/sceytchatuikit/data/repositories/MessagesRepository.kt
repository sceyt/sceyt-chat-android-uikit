package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.models.SceytPagingResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    suspend fun getPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, limit: Int): SceytResponse<List<SceytMessage>>
    suspend fun getNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, limit: Int): SceytResponse<List<SceytMessage>>
    suspend fun getNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean, limit: Int): SceytResponse<List<SceytMessage>>
    suspend fun getMessagesByType(channelId: Long, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>>
    suspend fun loadAllMessagesAfter(conversationId: Long, replyInThread: Boolean, messageId: Long): Flow<SceytResponse<List<SceytMessage>>>
    suspend fun searchMessages(conversationId: Long, replyInThread: Boolean, query: String): SceytPagingResponse<List<SceytMessage>>
    suspend fun loadNextSearchMessages(): SceytPagingResponse<List<SceytMessage>>
    suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>>
    suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult>
    suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: ((Message) -> Unit)? = null): SceytResponse<SceytMessage>
    suspend fun deleteMessage(channelId: Long, messageId: Long, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun markAsDisplayed(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun markAsReceived(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun getMessageById(channelId: Long, messageId: Long): SceytResponse<SceytMessage>
    suspend fun sendTypingState(channelId: Long, typing: Boolean)
}