package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

interface PersistenceMessagesMiddleWare {
    suspend fun loadMessages(channel: SceytChannel,
                             conversationId: Long,
                             lastMessageId: Long,
                             replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>>

    suspend fun sendMessage(channel: SceytChannel, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun deleteMessage(channel: SceytChannel, messageId: Long): SceytResponse<SceytMessage>
    suspend fun markAsRead(channel: SceytChannel, vararg ids: Long): SceytResponse<MessageListMarker>
}