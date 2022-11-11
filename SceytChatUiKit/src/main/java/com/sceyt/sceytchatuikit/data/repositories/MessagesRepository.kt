package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    suspend fun getPrevMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>>
    suspend fun getNextMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>>
    suspend fun getNearMessages(conversationId: Long, messageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>>
    suspend fun getMessagesByType(channelId: Long, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>>
    suspend fun loadAllMessagesAfter(conversationId: Long, replayInThread: Boolean, messageId: Long): Flow<SceytResponse<List<SceytMessage>>>
    suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit = {}): SceytResponse<SceytMessage?>
    suspend fun deleteMessage(channelId: Long, messageId: Long, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun markAsRead(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun markAsDelivered(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun sendTypingState(channelId: Long, typing: Boolean)
}