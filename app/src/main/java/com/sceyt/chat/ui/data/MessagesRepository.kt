package com.sceyt.chat.ui.data

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.models.messages.SceytReaction
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    suspend fun getMessages(lastMessageId: Long): SceytResponse<List<SceytMessage>>
    suspend fun getMessagesByType(lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>>
    suspend fun sendMessage(message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun deleteMessage(message: Message): SceytResponse<SceytMessage>
    suspend fun editMessage(message: Message): SceytResponse<SceytMessage>
    suspend fun addReaction(messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun markAsRead(id: Long): SceytResponse<MessageListMarker>
    suspend fun markAllAsRead(): SceytResponse<MessageListMarker>
    val onMessageFlow: Flow<SceytMessage>
    val onThreadMessageFlow: Flow<SceytMessage>
    val onMessageStatusFlow: Flow<MessageStatusChange>
    val onMessageReactionUpdatedFlow: Flow<Message>
    val onMessageEditedOrDeleteFlow: Flow<Message>
    val onChannelEventFlow: Flow<ChannelEventData>
}