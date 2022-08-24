package com.sceyt.chat.ui.persistence.logics

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

internal interface PersistenceMessagesLogic {
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>)
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    fun onMessageReactionUpdated(data: Message?)
    fun onMessageEditedOrDeleted(data: Message?)
    fun loadMessages(channel: SceytChannel, conversationId: Long, lastMessageId: Long,
                     replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>>

    suspend fun sendMessage(channel: SceytChannel, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?>
    suspend fun deleteMessage(channel: SceytChannel, messageId: Long): SceytResponse<SceytMessage>
}