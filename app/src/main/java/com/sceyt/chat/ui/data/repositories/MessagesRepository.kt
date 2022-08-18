package com.sceyt.chat.ui.data.repositories

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage

interface MessagesRepository {
    suspend fun getMessages(channel: Channel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>>
    suspend fun getMessagesByType(channel: Channel, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>>
    suspend fun sendMessage(channel: Channel, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun deleteMessage(channel: Channel, message: Message): SceytResponse<SceytMessage>
    suspend fun editMessage(channel: Channel, message: Message): SceytResponse<SceytMessage>
    suspend fun addReaction(channel: Channel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channel: Channel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun markAsRead(channel: Channel, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun sendTypingState(channel: Channel, typing: Boolean)
    suspend fun markAllAsRead(channel: Channel): SceytResponse<MessageListMarker>
    suspend fun join(channel: Channel): SceytResponse<SceytChannel>
}