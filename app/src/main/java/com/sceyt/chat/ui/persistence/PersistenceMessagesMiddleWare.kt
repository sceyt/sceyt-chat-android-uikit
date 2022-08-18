package com.sceyt.chat.ui.persistence

import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

interface PersistenceMessagesMiddleWare {
    suspend fun loadMessages(channel: SceytChannel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): Flow<PaginationResponse<SceytMessage>>
}