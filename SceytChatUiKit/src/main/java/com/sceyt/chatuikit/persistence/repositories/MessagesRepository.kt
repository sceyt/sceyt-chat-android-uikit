package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    suspend fun getPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                limit: Int): SceytResponse<List<SceytMessage>>

    suspend fun getNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                limit: Int): SceytResponse<List<SceytMessage>>

    suspend fun getNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                limit: Int): SceytResponse<List<SceytMessage>>

    suspend fun getMessagesByType(channelId: Long, lastMessageId: Long,
                                  type: String): SceytResponse<List<SceytMessage>>

    suspend fun loadAllMessagesAfter(conversationId: Long, replyInThread: Boolean,
                                     messageId: Long): Flow<Pair<Long, SceytResponse<List<SceytMessage>>>>

    suspend fun searchMessages(conversationId: Long, replyInThread: Boolean,
                               query: String): SceytPagingResponse<List<SceytMessage>>

    suspend fun loadNextSearchMessages(): SceytPagingResponse<List<SceytMessage>>
    suspend fun loadMessagesById(conversationId: Long,
                                 ids: List<Long>): SceytResponse<List<SceytMessage>>

    suspend fun sendMessage(channelId: Long, message: Message): SceytResponse<SceytMessage>
    suspend fun deleteMessage(channelId: Long, messageId: Long,
                              deleteType: DeleteMessageType): SceytResponse<SceytMessage>

    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun markMessageAs(channelId: Long, marker: MarkerType, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun addMessagesMarker(channelId: Long, marker: String, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun getMessageById(channelId: Long, messageId: Long): SceytResponse<SceytMessage>
}