package com.sceyt.sceytchatuikit.data.repositories

import android.util.Log
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
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class MessagesRepositoryImpl : MessagesRepository {

    private fun getQuery(conversationId: Long, replyInThread: Boolean, reversed: Boolean) = MessagesListQuery.Builder(conversationId).apply {
        setIsThread(replyInThread)
        setLimit(MESSAGES_LOAD_SIZE)
        setReversed(reversed)
    }.build()

    private fun getQueryByType(type: String, conversationId: Long) = MessagesListQueryByType.Builder(conversationId, type).apply {
        limit(MESSAGES_LOAD_SIZE)
        reversed(true)
    }.build()

    /**
     * @param conversationId id of current conversation, if is reply in thread, it is the reply message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replyInThread reply message in thread mode. */
    override suspend fun getPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replyInThread, true).loadPrev(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && lastMessageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Error(e))
                        Log.e(TAG, "getPrevMessages error: ${e?.message}")
                    }
                }
            })
        }
    }

    /**
     * @param conversationId id of current conversation, if is reply in thread, it is the reply message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replyInThread reply message in thread mode. */
    override suspend fun getNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replyInThread, false).loadNext(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && lastMessageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Error(e))
                        Log.e(TAG, "getNextMessages error: ${e?.message}")
                    }
                }
            })
        }
    }

    /**
     * @param conversationId id of current conversation, if is reply in thread, it is the reply message id, else channel id.
     * @param messageId conversation last message id.
     * @param replyInThread reply message in thread mode. */
    override suspend fun getNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replyInThread, true).loadNear(messageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replyInThread && messageId == 0L)
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Error(e))
                        Log.e(TAG, "getNearMessages error: ${e?.message}")
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
            getQueryByType(type, channelId).loadNext(lastMsgId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.safeResume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "getMessagesByType error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun loadAllMessagesAfter(conversationId: Long, replyInThread: Boolean,
                                              messageId: Long): Flow<SceytResponse<List<SceytMessage>>> = callbackFlow {
        val query = getQuery(conversationId, replyInThread, false)

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
                Log.e(TAG, "loadAllMessagesAfter error: ${e?.message}")
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
                    Log.e(TAG, "loadMessages error: ${error?.message}")
                }
            })
        }
    }

    override suspend fun sendMessageAsFlow(channelId: Long, message: Message) = callbackFlow {
        var tmpMessage: Message? = null
        tmpMessage = ChannelOperator.build(channelId).sendMessage(message, object : MessageCallback {
            override fun onResult(message: Message) {
                trySend(SendMessageResult.Response(SceytResponse.Success(message.toSceytUiMessage())))
                channel.close()
            }

            override fun onError(error: SceytException?) {
                trySend(SendMessageResult.Response(SceytResponse.Error(error, data = tmpMessage?.toSceytUiMessage())))
                channel.close()
                Log.e(TAG, "sendMessageAsFlow error: ${error?.message}")
            }
        })
        trySend(SendMessageResult.TempMessage(tmpMessage.toSceytUiMessage()))
        awaitClose()
    }

    override suspend fun sendMessage(channelId: Long, message: Message): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).sendMessage(message, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.safeResume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    Log.e(TAG, "sendMessage error: ${error?.message}")
                }
            })
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
                    Log.e(TAG, "deleteMessage error: ${ex?.message}")
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
                    Log.e(TAG, "editMessage error: ${ex?.message}")
                }
            })
        }
    }

    override suspend fun markAsRead(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markMessagesAsRead(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.safeResume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    Log.e(TAG, "markAsRead error: ${error?.message}")
                }
            })
        }
    }

    override suspend fun markAsDelivered(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markMessagesAsDelivered(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.safeResume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    Log.e(TAG, "markAsDelivered error: ${error?.message}")
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