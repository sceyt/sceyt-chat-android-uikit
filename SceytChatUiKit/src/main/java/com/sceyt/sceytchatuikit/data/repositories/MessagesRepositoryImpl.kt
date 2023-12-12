package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessagesListQuery
import com.sceyt.chat.models.message.MessagesListQueryByType
import com.sceyt.chat.operators.ChannelOperator
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chat.sceyt_callbacks.MessageMarkCallback
import com.sceyt.chat.sceyt_callbacks.MessagesCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class MessagesRepositoryImpl : MessagesRepository {

    private fun getQuery(conversationId: Long, replyInThread: Boolean, limit: Int, reversed: Boolean) = MessagesListQuery.Builder(conversationId).apply {
        setIsThread(replyInThread)
        setLimit(limit)
        setReversed(reversed)
    }.build()

    private fun getQueryByType(type: String, conversationId: Long, limit: Int) = MessagesListQueryByType.Builder(conversationId, type).apply {
        limit(limit)
        reversed(true)
    }.build()

    /**
     * @param conversationId id of current conversation, if is reply in thread, it is the reply message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replyInThread reply message in thread mode.
     * @param limit count of messages. */
    override suspend fun getPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, limit: Int): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replyInThread, limit, true).loadPrev(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && lastMessageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Error(e))
                        SceytLog.e(TAG, "getPrevMessages error: ${e?.message}")
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
    override suspend fun getNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, limit: Int): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replyInThread, limit, false).loadNext(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && lastMessageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Error(e))
                        SceytLog.e(TAG, "getNextMessages error: ${e?.message}")
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
    override suspend fun getNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean, limit: Int): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replyInThread, limit, true).loadNear(messageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && messageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Error(e))
                        SceytLog.e(TAG, "getNearMessages error: ${e?.message}")
                    }
                }
            })
        }
    }

    /**
     * @param channelId the main channel id.
     * @param lastMessageId conversation last message id.
     * @param type messages type. */
    override suspend fun getMessagesByType(channelId: Long, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>> {
        val lastMsgId = if (lastMessageId == 0L) Long.MAX_VALUE else lastMessageId
        return suspendCancellableCoroutine { continuation ->
            getQueryByType(type, channelId, MESSAGES_LOAD_SIZE).loadNext(lastMsgId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getMessagesByType error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun loadAllMessagesAfter(conversationId: Long, replyInThread: Boolean,
                                              messageId: Long): Flow<SceytResponse<List<SceytMessage>>> = callbackFlow {
        val query = getQuery(conversationId, replyInThread, MESSAGES_LOAD_SIZE, false)

        query.loadNext(messageId, object : MessagesCallback {
            override fun onResult(messages: MutableList<Message>?) {
                val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                trySend(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                if (result.size == MESSAGES_LOAD_SIZE) {
                    query.loadNext(result.last().id, this)
                } else channel.close()
            }

            override fun onError(e: SceytException?) {
                trySend(SceytResponse.Error(e))
                channel.close()
                SceytLog.e(TAG, "loadAllMessagesAfter error: ${e?.message}")
            }
        })

        awaitClose()
    }

    override suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(conversationId).getMessagesById(ids.toLongArray(), object : MessagesCallback {
                override fun onResult(result: MutableList<Message>?) {
                    continuation.safeResume(SceytResponse.Success(result?.map { it.toSceytUiMessage() }
                            ?: emptyList()))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "loadMessages error: ${error?.message}")
                }
            })
        }
    }

    override suspend fun sendMessageAsFlow(channelId: Long, message: Message) = callbackFlow {
        val response = sendMessage(channelId, message, tmpMessageCb = {
            trySend(SendMessageResult.TempMessage(it.toSceytUiMessage()))
        })
        when (response) {
            is SceytResponse.Success -> trySend(SendMessageResult.Success(response))
            is SceytResponse.Error -> trySend(SendMessageResult.Error(response))
        }
        channel.close()
        awaitClose()
    }

    override suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: ((Message) -> Unit)?): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            val transformMessage = SceytKitConfig.messageTransformer?.transformToSend(message)
                    ?: message
            SceytLog.i(TAG, "sending message with channelId $channelId, tid: ${transformMessage.tid}, body: ${transformMessage.body}")
            val tmpMessage = ChannelOperator.build(channelId).sendMessage(transformMessage, object : MessageCallback {
                override fun onResult(message: Message) {
                    SceytLog.i(TAG, "send message success with tid: ${message.tid}, body: ${message.body}, initialTid: ${transformMessage.tid}")
                    val resultTransformed = SceytKitConfig.messageTransformer?.transformToGet(message)
                            ?: message
                    continuation.safeResume(SceytResponse.Success(resultTransformed.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "sendMessage error: ${error?.message}, messageTid: " +
                            "${transformMessage.tid}, body: ${transformMessage.body}")
                }
            })
            tmpMessageCb?.invoke(tmpMessage)
        }
    }

    override suspend fun deleteMessage(channelId: Long, messageId: Long, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).deleteMessage(messageId, onlyForMe, object : MessageCallback {
                override fun onResult(msg: Message) {
                    continuation.safeResume(SceytResponse.Success(msg.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(ex))
                    SceytLog.e(TAG, "deleteMessage error: ${ex?.message}")
                }
            })
        }
    }

    override suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).editMessage(message.toMessage(), object : MessageCallback {
                override fun onResult(result: Message) {
                    continuation.safeResume(SceytResponse.Success(result.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(ex))
                    SceytLog.e(TAG, "editMessage error: ${ex?.message}")
                }
            })
        }
    }

    override suspend fun markAsDisplayed(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markMessagesAsDisplayed(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.safeResume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "markAsRead error: ${error?.message}")
                }
            })
        }
    }

    override suspend fun markAsReceived(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markMessagesAsReceived(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.safeResume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "markAsDelivered error: ${error?.message}")
                }
            })
        }
    }

    override suspend fun getMessageById(channelId: Long, messageId: Long): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).getMessagesById(longArrayOf(messageId), object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    if (!messages.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(messages.first().toSceytUiMessage()))
                    else continuation.safeResume(SceytResponse.Error(SceytException(0, "Message not found")))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "getMessageById error: ${error?.message}")
                }
            })
        }
    }

    override suspend fun sendTypingState(channelId: Long, typing: Boolean) {
        if (typing)
            ChannelOperator.build(channelId).startTyping()
        else ChannelOperator.build(channelId).stopTyping()
    }
}