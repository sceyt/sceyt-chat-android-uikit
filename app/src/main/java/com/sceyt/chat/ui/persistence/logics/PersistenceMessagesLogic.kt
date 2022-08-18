package com.sceyt.chat.ui.persistence.logics

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

internal interface PersistenceMessagesLogic {
    fun onMessage(data: Pair<Channel, Message>)
    fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    fun onMessageReactionUpdated(data: Message?)
    fun onMessageEditedOrDeleted(data: Message?)
    fun loadMessages(channel: SceytChannel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): Flow<PaginationResponse<SceytMessage>>
}