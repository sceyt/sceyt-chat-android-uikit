package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

interface MessagesRepository {
    suspend fun getMessages(channel: SceytChannel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>>
    suspend fun getMessagesByType(channel: SceytChannel, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>>
    suspend fun sendMessage(channel: SceytChannel, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun deleteMessage(channel: SceytChannel, messageId: Long): SceytResponse<SceytMessage>
    suspend fun editMessage(channel: SceytChannel, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun addReaction(channel: SceytChannel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channel: SceytChannel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun markAsRead(channel: SceytChannel, vararg id: Long): SceytResponse<MessageListMarker>
    suspend fun sendTypingState(channel: SceytChannel, typing: Boolean)
    suspend fun markAllAsRead(channel: SceytChannel): SceytResponse<MessageListMarker>
    suspend fun join(channel: SceytChannel): SceytResponse<SceytChannel>
}