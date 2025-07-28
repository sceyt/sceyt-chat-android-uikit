package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chat.models.Types.Direction
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SendMessageResult
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface MessageInteractor {
    suspend fun loadPrevMessages(
            conversationId: Long, lastMessageId: Long,
            replyInThread: Boolean, offset: Int,
            limit: Int = SceytChatUIKit.config.queryLimits.messageListQueryLimit,
            loadKey: LoadKeyData, ignoreDb: Boolean = false,
    ): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNextMessages(
            conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
            offset: Int, limit: Int = SceytChatUIKit.config.queryLimits.messageListQueryLimit,
            ignoreDb: Boolean = false,
    ): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNearMessages(
            conversationId: Long, messageId: Long, replyInThread: Boolean,
            limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean = false,
            ignoreServer: Boolean = false,
    ): Flow<PaginationResponse<SceytMessage>>

    suspend fun loadNewestMessages(
            conversationId: Long, replyInThread: Boolean,
            limit: Int = SceytChatUIKit.config.queryLimits.messageListQueryLimit,
            loadKey: LoadKeyData, ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytMessage>>

    suspend fun searchMessages(
            conversationId: Long, replyInThread: Boolean, query: String,
    ): SceytPagingResponse<List<SceytMessage>>

    suspend fun getUnreadMentions(
            conversationId: Long,
            direction: Direction,
            messageId: Long,
            limit: Int = SceytChatUIKit.config.queryLimits.unreadMentionsListQueryLimit,
    ): SceytPagingResponse<List<Long>>

    suspend fun loadNextSearchMessages(): SceytPagingResponse<List<SceytMessage>>
    suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>>

    suspend fun syncMessagesAfterMessageId(
            conversationId: Long, replyInThread: Boolean,
            messageId: Long,
    ): Flow<SceytResponse<List<SceytMessage>>>

    suspend fun syncNearMessages(
            conversationId: Long, messageId: Long,
            replyInThread: Boolean,
    ): SyncNearMessagesResult

    suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult>
    suspend fun sendMessage(channelId: Long, message: Message)
    suspend fun sendMessages(channelId: Long, messages: List<Message>)
    suspend fun sendSharedFileMessage(channelId: Long, message: Message)
    suspend fun sendFrowardMessages(channelId: Long, vararg messagesToSend: Message): SceytResponse<Boolean>
    suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message): SceytResponse<SceytMessage>
    suspend fun sendPendingMessages(channelId: Long)
    suspend fun sendAllPendingMessages()
    suspend fun sendAllPendingMarkers()
    suspend fun sendAllPendingMessageStateUpdates()
    suspend fun sendAllPendingReactions()
    suspend fun markMessagesAs(
            channelId: Long, marker: MarkerType,
            vararg ids: Long,
    ): List<SceytResponse<MessageListMarker>>

    suspend fun addMessagesMarker(
            channelId: Long, marker: String,
            vararg ids: Long,
    ): List<SceytResponse<MessageListMarker>>

    suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage>
    suspend fun deleteMessage(
            channelId: Long, message: SceytMessage,
            deleteType: DeleteMessageType,
    ): SceytResponse<SceytMessage>

    suspend fun getMessageFromServerById(channelId: Long, messageId: Long): SceytResponse<SceytMessage>
    suspend fun getMessageFromDbById(messageId: Long): SceytMessage?
    suspend fun getMessageFromDbByTid(messageTid: Long): SceytMessage?
    suspend fun sendChannelEvent(channelId: Long, event: String)
    fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>>
}