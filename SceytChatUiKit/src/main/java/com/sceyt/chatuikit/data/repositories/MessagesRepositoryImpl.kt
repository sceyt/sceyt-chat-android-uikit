package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.SearchQueryOperator
import com.sceyt.chat.models.Types
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListFilterKey
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessageSearchField
import com.sceyt.chat.models.message.MessageSearchQuery
import com.sceyt.chat.models.message.MessagesListQuery
import com.sceyt.chat.models.message.MessagesListQueryByType
import com.sceyt.chat.models.message.UnreadMentionsListQuery
import com.sceyt.chat.operators.ChannelOperator
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chat.sceyt_callbacks.MessageIdsCallback
import com.sceyt.chat.sceyt_callbacks.MessageMarkCallback
import com.sceyt.chat.sceyt_callbacks.MessagesCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.retryOnResendableError
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class MessagesRepositoryImpl : MessagesRepository {
    private var searchMessageListQuery: MessagesListQuery? = null

    /**
     * @param conversationId id of current conversation, if is reply in thread, it is the reply message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replyInThread reply message in thread mode.
     * @param limit count of messages. */
    override suspend fun getPrevMessages(
        conversationId: Long,
        lastMessageId: Long,
        replyInThread: Boolean,
        limit: Int
    ): SceytResponse<List<SceytMessage>> = retryOnResendableError {
        getPrevMessagesImpl(conversationId, lastMessageId, replyInThread, limit)
    }

    private suspend fun getPrevMessagesImpl(
        conversationId: Long,
        lastMessageId: Long,
        replyInThread: Boolean,
        limit: Int
    ): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(
                conversationId = conversationId,
                replyInThread = replyInThread,
                limit = limit,
                reversed = true
            ).loadPrev(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result = messages.orEmpty()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && lastMessageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        SceytLog.e(
                            TAG,
                            "getPrevMessages error: ${e?.message}, channelId: $conversationId, lastMessageId: $lastMessageId, replyInThread: $replyInThread"
                        )
                        continuation.safeResume(SceytResponse.Error(e))
                    }
                }
            })
        }
    }

    /**
     * @param conversationId id of current conversation, if is reply in thread, it is the reply message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replyInThread reply message in thread mode.
     * @param limit count of messages */
    override suspend fun getNextMessages(
        conversationId: Long,
        lastMessageId: Long,
        replyInThread: Boolean,
        limit: Int
    ): SceytResponse<List<SceytMessage>> = retryOnResendableError {
        getNextMessagesImpl(conversationId, lastMessageId, replyInThread, limit)
    }

    private suspend fun getNextMessagesImpl(
        conversationId: Long,
        lastMessageId: Long,
        replyInThread: Boolean,
        limit: Int
    ): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(
                conversationId = conversationId,
                replyInThread = replyInThread,
                limit = limit,
                reversed = false
            ).loadNext(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result = messages.orEmpty()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && lastMessageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        SceytLog.e(
                            TAG,
                            "getNextMessages error: ${e?.message}, channelId: $conversationId, lastMessageId: $lastMessageId, replyInThread: $replyInThread"
                        )
                        continuation.safeResume(SceytResponse.Error(e))
                    }
                }
            })
        }
    }

    /**
     * @param conversationId id of current conversation, if is reply in thread, it is the reply message id, else channel id.
     * @param messageId conversation last message id.
     * @param replyInThread reply message in thread mode.
     * @param limit count of messages */
    override suspend fun getNearMessages(
        conversationId: Long,
        messageId: Long,
        replyInThread: Boolean,
        limit: Int
    ): SceytResponse<List<SceytMessage>> = retryOnResendableError {
        getNearMessagesImpl(conversationId, messageId, replyInThread, limit)
    }

    private suspend fun getNearMessagesImpl(
        conversationId: Long,
        messageId: Long,
        replyInThread: Boolean,
        limit: Int
    ): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(
                conversationId = conversationId,
                replyInThread = replyInThread,
                limit = limit,
                reversed = true
            ).loadNear(messageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result = messages.orEmpty()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && messageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        SceytLog.e(
                            TAG,
                            "getNearMessages error: ${e?.message}, channelId: $conversationId, messageId: $messageId, replyInThread: $replyInThread"
                        )
                        continuation.safeResume(SceytResponse.Error(e))
                    }
                }
            })
        }
    }

    /**
     * @param channelId the main channel id.
     * @param lastMessageId conversation last message id.
     * @param type messages type. */
    override suspend fun getMessagesByType(
        channelId: Long,
        lastMessageId: Long,
        type: String,
        limit: Int
    ): SceytResponse<List<SceytMessage>> {
        val lastMsgId = if (lastMessageId == 0L) Long.MAX_VALUE else lastMessageId
        return suspendCancellableCoroutine { continuation ->
            getQueryByType(
                type = type,
                conversationId = channelId,
                limit = limit
            ).loadNext(lastMsgId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result = messages.orEmpty()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    SceytLog.e(
                        TAG,
                        "getMessagesByType error: ${e?.message}, channelId: $channelId, lastMessageId: $lastMessageId, type: $type"
                    )
                    continuation.safeResume(SceytResponse.Error(e))
                }
            })
        }
    }

    override suspend fun syncMessagesAfterMessageId(
        conversationId: Long,
        replyInThread: Boolean,
        messageId: Long,
        limit: Int,
    ): Flow<SyncResult<SceytMessage>> = callbackFlow {
        val query = getQuery(conversationId, replyInThread, limit, false)

        query.loadNext(messageId - 1, object : MessagesCallback {
            override fun onResult(messages: MutableList<Message>?) {
                val result = messages.orEmpty()
                trySend(SyncResult.Proportion(result.map { it.toSceytUiMessage() }))
                if (result.size == limit) {
                    query.loadNext(result.last().id, this)
                } else {
                    trySend(SyncResult.SuccessfullyFinished)
                    channel.close()
                }
            }

            override fun onError(e: SceytException?) {
                trySend(SyncResult.Error(e))
                SceytLog.e(
                    TAG,
                    "syncMessagesAfterMessageId error: ${e?.message}, channelId: $conversationId, messageId: $messageId, replyInThread: $replyInThread"
                )
                channel.close()
            }
        })

        awaitClose()
    }

    override suspend fun loadMessagesById(
        conversationId: Long,
        ids: List<Long>
    ): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(conversationId)
                .getMessagesById(ids.toLongArray(), object : MessagesCallback {
                    override fun onResult(result: MutableList<Message>?) {
                        continuation.safeResume(SceytResponse.Success(result?.map { it.toSceytUiMessage() }
                            .orEmpty()))
                    }

                    override fun onError(error: SceytException?) {
                        SceytLog.e(
                            TAG,
                            "loadMessages error: ${error?.message}, channelId: $conversationId, ids: $ids"
                        )
                        continuation.safeResume(SceytResponse.Error(error))
                    }
                })
        }
    }

    override suspend fun searchMessages(
        conversationId: Long,
        replyInThread: Boolean,
        query: String
    ): SceytPagingResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            val searchField = MessageSearchField.Builder()
                .withFilterKey(MessageListFilterKey.FilterKeyBody)
                .queryType(SearchQueryOperator.SearchQueryOperatorContains)
                .query(query)
                .build()
            val searchQuery = MessageSearchQuery(listOf(searchField))

            searchMessageListQuery = getQueryForSearch(
                conversationId = conversationId,
                replyInThread = replyInThread,
                limit = messagesLoadSize,
                searchQuery = searchQuery
            )
            searchMessageListQuery?.loadNext(object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    continuation.safeResume(SceytPagingResponse.Success((messages?.map {
                        it.toSceytUiMessage()
                    }.orEmpty()), searchMessageListQuery?.hasNext ?: false))
                }

                override fun onError(error: SceytException?) {
                    SceytLog.e(
                        TAG,
                        "searchMessages error: ${error?.message}, channelId: $conversationId, replyInThread: $replyInThread, query: $query"
                    )
                    continuation.safeResume(SceytPagingResponse.Error(error))
                }
            })
        }
    }

    override suspend fun loadNextSearchMessages(): SceytPagingResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            searchMessageListQuery?.loadNext(object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    continuation.safeResume(
                        SceytPagingResponse.Success(
                            data = messages?.map { it.toSceytUiMessage() }.orEmpty(),
                            hasNext = searchMessageListQuery?.hasNext ?: false
                        )
                    )
                }

                override fun onError(error: SceytException?) {
                    SceytLog.e(TAG, "loadNextSearchMessages error: ${error?.message}")
                    continuation.safeResume(SceytPagingResponse.Error(error))
                }
            })
        }
    }

    override suspend fun getUnreadMentions(
        conversationId: Long,
        direction: Types.Direction,
        messageId: Long,
        limit: Int,
    ): SceytPagingResponse<List<Long>> = suspendCancellableCoroutine { continuation ->
        val query = UnreadMentionsListQuery.Builder(conversationId)
            .setMessageId(messageId)
            .setDirection(direction)
            .setLimit(limit)
            .build()

        query.load(object : MessageIdsCallback {
            override fun onResult(messageIds: MutableList<Long>?) {
                continuation.safeResume(
                    SceytPagingResponse.Success(
                        data = messageIds.orEmpty(),
                        hasNext = query.hasNext
                    )
                )
            }

            override fun onError(e: SceytException?) {
                SceytLog.e(
                    TAG,
                    "getUnreadMentions error: ${e?.message}, conversationId: $conversationId, direction: $direction, messageId: $messageId, limit: $limit"
                )
                continuation.safeResume(SceytPagingResponse.Error(e))
            }
        })
    }

    override suspend fun sendMessage(
        channelId: Long,
        sceytMessage: SceytMessage,
    ): SceytResponse<SceytMessage> = retryOnResendableError {
        sendMessageImpl(channelId, sceytMessage)
    }

    private suspend fun sendMessageImpl(
        channelId: Long,
        sceytMessage: SceytMessage,
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        val message = sceytMessage.toMessage()
        val transformMessage = SceytChatUIKit.messageTransformer?.transformToSend(message)
            ?: message
        SceytLog.i(TAG, "sending message with channelId $channelId, tid: ${message.tid}")
        ChannelOperator.build(channelId).sendMessage(transformMessage, object : MessageCallback {
            override fun onResult(message: Message) {
                SceytLog.i(
                    TAG,
                    "send message success with tid: ${message.tid}, initialTid: ${message.tid}, channelId: $channelId"
                )
                val resultTransformed = SceytChatUIKit.messageTransformer?.transformToGet(message)
                    ?: message
                continuation.safeResume(SceytResponse.Success(resultTransformed.toSceytUiMessage()))
            }

            override fun onError(error: SceytException?) {
                SceytLog.e(
                    TAG,
                    "sendMessage error: ${error?.message}, messageTid: ${message.tid}, channelId: $channelId"
                )
                continuation.safeResume(SceytResponse.Error(error))
            }
        })
    }

    override suspend fun deleteMessage(
        channelId: Long,
        messageId: Long,
        deleteType: DeleteMessageType,
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        ChannelOperator.build(channelId)
            .deleteMessage(messageId, deleteType, object : MessageCallback {
                override fun onResult(msg: Message) {
                    continuation.safeResume(SceytResponse.Success(msg.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    SceytLog.e(
                        TAG,
                        "deleteMessage error: ${ex?.message}, messageId: $messageId, channelId: $channelId, deleteType: $deleteType"
                    )
                    continuation.safeResume(SceytResponse.Error(ex))
                }
            })
    }

    override suspend fun deleteMessageByTid(
        channelId: Long,
        tid: Long,
        deleteType: DeleteMessageType,
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        // Build a message with id = 0 and only the tid set, so the SDK deletes it by tid.
        val message = Message.MessageBuilder(channelId).setTid(tid).build()
        ChannelOperator.build(channelId)
            .deleteMessage(message, deleteType, object : MessageCallback {
                override fun onResult(msg: Message) {
                    continuation.safeResume(SceytResponse.Success(msg.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    SceytLog.e(
                        TAG,
                        "deleteMessageByTid error: ${ex?.message}, tid: $tid, channelId: $channelId, deleteType: $deleteType"
                    )
                    continuation.safeResume(SceytResponse.Error(ex))
                }
            })
    }

    override suspend fun editMessage(
        channelId: Long,
        message: SceytMessage
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        ChannelOperator.build(channelId)
            .editMessage(message.toMessage(), object : MessageCallback {
                override fun onResult(result: Message) {
                    continuation.safeResume(SceytResponse.Success(result.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    SceytLog.e(
                        TAG,
                        "editMessage error: ${ex?.message}, messageId: ${message.id}, channelId: $channelId"
                    )
                    continuation.safeResume(SceytResponse.Error(ex))
                }
            })
    }

    override suspend fun markMessageAs(
        channelId: Long,
        marker: MarkerType,
        vararg id: Long
    ): SceytResponse<MessageListMarker> = suspendCancellableCoroutine { continuation ->
        val callback = object : MessageMarkCallback {
            override fun onResult(result: MessageListMarker) {
                continuation.safeResume(SceytResponse.Success(result))
            }

            override fun onError(error: SceytException?) {
                SceytLog.e(
                    TAG,
                    "markAs:${marker} error: ${error?.message}, channelId: $channelId, ids: ${
                        id.joinToString(",")
                    }"
                )
                continuation.safeResume(SceytResponse.Error(error))
            }
        }
        val channelOperator = ChannelOperator.build(channelId)
        when (marker) {
            MarkerType.Displayed -> channelOperator.markMessagesAsDisplayed(id, callback)
            MarkerType.Received -> channelOperator.markMessagesAsReceived(id, callback)
            else -> channelOperator.markMessages(id, marker.value, callback)
        }
    }

    override suspend fun addMessagesMarker(
        channelId: Long,
        marker: String,
        vararg id: Long
    ): SceytResponse<MessageListMarker> = suspendCancellableCoroutine { continuation ->
        val channelOperator = ChannelOperator.build(channelId)
        channelOperator.markMessages(id, marker, object : MessageMarkCallback {
            override fun onResult(result: MessageListMarker) {
                continuation.safeResume(SceytResponse.Success(result))
            }

            override fun onError(error: SceytException?) {
                SceytLog.e(
                    TAG,
                    "addMessagesMarker: $marker error: ${error?.message}, channelId: $channelId, ids: ${
                        id.joinToString(",")
                    }"
                )
                continuation.safeResume(SceytResponse.Error(error))
            }
        })
    }

    override suspend fun getMessageById(
        channelId: Long,
        messageId: Long
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        ChannelOperator.build(channelId)
            .getMessagesById(longArrayOf(messageId), object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    if (!messages.isNullOrEmpty()) {
                        continuation.safeResume(
                            SceytResponse.Success(
                                messages.first().toSceytUiMessage()
                            )
                        )
                    } else continuation.safeResume(createErrorResponse("Message not found. messageId: $messageId, channelId: $channelId"))
                }

                override fun onError(error: SceytException?) {
                    SceytLog.e(
                        TAG,
                        "getMessageById error: ${error?.message}, messageId: $messageId, channelId: $channelId"
                    )
                    continuation.safeResume(SceytResponse.Error(error))
                }
            })
    }

    private val messagesLoadSize get() = SceytChatUIKit.config.queryLimits.messageListQueryLimit

    private fun getQuery(
        conversationId: Long,
        replyInThread: Boolean,
        limit: Int,
        reversed: Boolean,
    ) = MessagesListQuery.Builder(conversationId)
        .setIsThread(replyInThread)
        .setLimit(limit)
        .setReversed(reversed)
        .build()

    private fun getQueryForSearch(
        conversationId: Long,
        replyInThread: Boolean,
        limit: Int,
        searchQuery: MessageSearchQuery,
    ) = MessagesListQuery.Builder(conversationId)
        .setIsThread(replyInThread)
        .setLimit(limit)
        .setReversed(true)
        .setSearchQuery(searchQuery)
        .build()

    private fun getQueryByType(
        type: String,
        conversationId: Long,
        limit: Int,
    ) = MessagesListQueryByType.Builder(conversationId, type)
        .limit(limit)
        .reversed(true)
        .build()
}