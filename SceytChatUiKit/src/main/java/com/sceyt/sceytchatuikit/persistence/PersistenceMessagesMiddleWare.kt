package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

interface PersistenceMessagesMiddleWare {
    suspend fun loadPrevMessages(conversationId: Long,
                                 lastMessageId: Long,
                                 replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>>
    suspend fun loadNextMessages(conversationId: Long,
                                 lastMessageId: Long,
                                 replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>>
    suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun markAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker>
    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun loadNearMessages(conversationId: Long, messageId: Long, replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>>
}