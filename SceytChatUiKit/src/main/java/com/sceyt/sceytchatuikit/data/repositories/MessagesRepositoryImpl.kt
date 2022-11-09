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
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessagesRepositoryImpl : MessagesRepository {

    private fun getQuery(conversationId: Long, replayInThread: Boolean, reversed: Boolean) = MessagesListQuery.Builder(conversationId).apply {
        setIsThread(replayInThread)
        setLimit(MESSAGES_LOAD_SIZE)
        setReversed(reversed)
    }.build()

    private fun getQueryByType(type: String, conversationId: Long) = MessagesListQueryByType.Builder(conversationId, type).apply {
        limit(MESSAGES_LOAD_SIZE)
        reversed(true)
    }.build()

    /**
     * @param conversationId id of current conversation, if is replay in thread, it is the replay message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replayInThread replay message in thread mode. */
    override suspend fun getPrevMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replayInThread, true).loadPrev(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.resume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replayInThread && lastMessageId == 0L)
                        continuation.resume(SceytResponse.Success(arrayListOf()))
                    else
                        continuation.resume(SceytResponse.Error(e))
                }
            })
        }
    }

    /**
     * @param conversationId id of current conversation, if is replay in thread, it is the replay message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replayInThread replay message in thread mode. */
    override suspend fun getNextMessages(conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replayInThread, false).loadNext(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.resume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replayInThread && lastMessageId == 0L)
                        continuation.resume(SceytResponse.Success(arrayListOf()))
                    else
                        continuation.resume(SceytResponse.Error(e))
                }
            })
        }
    }

    /**
     * @param conversationId id of current conversation, if is replay in thread, it is the replay message id, else channel id.
     * @param messageId conversation last message id.
     * @param replayInThread replay message in thread mode. */
    override suspend fun getNearMessages(conversationId: Long, messageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replayInThread, true).loadNear(messageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.resume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replayInThread && messageId == 0L)
                        continuation.resume(SceytResponse.Success(arrayListOf()))
                    else
                        continuation.resume(SceytResponse.Error(e))
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
                    continuation.resume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e))
                }
            })
        }
    }

    override suspend fun loadAllMessagesAfter(conversationId: Long, replayInThread: Boolean,
                                              messageId: Long): Flow<SceytResponse<List<SceytMessage>>> = callbackFlow {
        val query = getQuery(conversationId, replayInThread, false)

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
            }
        })

        awaitClose()
    }

    override suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?> {
        return suspendCancellableCoroutine { continuation ->
            var tmpMessage: Message? = null
            tmpMessage = ChannelOperator.build(channelId).sendMessage(message, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error, data = tmpMessage?.toSceytUiMessage()))
                }
            })
            tmpMessageCb.invoke(tmpMessage)
        }
    }

    override suspend fun deleteMessage(channelId: Long, messageId: Long, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).deleteMessage(messageId, onlyForMe, object : MessageCallback {
                override fun onResult(msg: Message) {
                    continuation.resume(SceytResponse.Success(msg.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.resume(SceytResponse.Error(ex))
                }
            })
        }
    }

    override suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).editMessage(message.toMessage(), object : MessageCallback {
                override fun onResult(result: Message) {
                    continuation.resume(SceytResponse.Success(result.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.resume(SceytResponse.Error(ex))
                }
            })
        }
    }

    override suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).addReactionWithMessageId(messageId, scoreKey, 1, "", false, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error))
                }
            })
        }
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).deleteReactionWithMessageId(messageId, scoreKey, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error))
                }
            })
        }
    }

    override suspend fun markAsRead(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markMessagesAsRead(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.resume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error))
                }
            })
        }
    }

    override suspend fun markAsDelivered(channelId: Long, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markMessagesAsDelivered(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.resume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error))
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