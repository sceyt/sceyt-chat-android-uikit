package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

internal interface PersistenceMessagesLogic {
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>)
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    fun onMessageReactionUpdated(data: Message?)
    fun onMessageEditedOrDeleted(data: Message?)
    suspend fun loadMessages(conversationId: Long, lastMessageId: Long,
                     replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>>

    suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun deleteMessage(channelId: Long, messageId: Long, messageTid: Long, onlyForMe: Boolean): SceytResponse<SceytMessage>
    suspend fun markAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker>
    suspend fun editMessage(id: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
}