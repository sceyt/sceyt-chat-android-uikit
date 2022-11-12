package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

internal interface PersistenceMessagesLogic {
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>)
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    suspend fun onMessageReactionUpdated(data: Message)
    suspend fun onMessageEditedOrDeleted(data: Message)
    suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean, offset: Int,
                                 loadKey: Long, ignoreDb: Boolean, ignoreCash: Boolean = false): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean, offset: Int,
                                 ignoreDb: Boolean, ignoreCash: Boolean = false): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNearMessages(conversationId: Long, messageId: Long, replayInThread: Boolean, loadKey: Long,
                                 ignoreDb: Boolean, ignoreCash: Boolean = false): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNewestMessages(conversationId: Long, replayInThread: Boolean, loadKey: Long,
                                   ignoreDb: Boolean, ignoreCash: Boolean = false): Flow<PaginationResponse<SceytMessage>>

    suspend fun syncMessagesAfterMessageId(conversationId: Long, replayInThread: Boolean,
                                           messageId: Long): Flow<SceytResponse<List<SceytMessage>>>

    suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun sendAllPendingMarkers()
    suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun markMessageAsDelivered(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker>
    suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker>
    suspend fun editMessage(id: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>>
}