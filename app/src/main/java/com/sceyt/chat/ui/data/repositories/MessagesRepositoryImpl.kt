package com.sceyt.chat.ui.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.PublicChannel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessagesListQuery
import com.sceyt.chat.models.message.MessagesListQueryByType
import com.sceyt.chat.sceyt_callbacks.ChannelCallback
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chat.sceyt_callbacks.MessageMarkCallback
import com.sceyt.chat.sceyt_callbacks.MessagesCallback
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.toSceytUiChannel
import com.sceyt.chat.ui.data.toSceytUiMessage
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessagesRepositoryImpl : MessagesRepository {

    private fun getQuery(conversationId: Long, replayInThread: Boolean) = MessagesListQuery.Builder(conversationId).apply {
        setIsThread(replayInThread)
        setLimit(MESSAGES_LOAD_SIZE)
    }.build()

    private fun getQueryByType(type: String, conversationId: Long) = MessagesListQueryByType.Builder(conversationId, type).apply {
        limit(MESSAGES_LOAD_SIZE)
        reversed(true)
    }.build()

    /**
     * @param channel the main channel.
     * @param conversationId id of current conversation, if is replay in thread, it is the replay message id, else channel id.
     * @param lastMessageId conversation last message id.
     * @param replayInThread replay message in thread mode. */
    override suspend fun getMessages(channel: Channel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, replayInThread).loadPrev(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.resume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    if (replayInThread && lastMessageId == 0L)
                        continuation.resume(SceytResponse.Success(arrayListOf()))
                    else
                        continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    /**
     * @param channel the main channel.
     * @param lastMessageId conversation last message id.
     * @param type messages type. */
    override suspend fun getMessagesByType(channel: Channel, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>> {
        val lastMsgId = if (lastMessageId == 0L) Long.MAX_VALUE else lastMessageId
        return suspendCancellableCoroutine { continuation ->
            getQueryByType(type, channel.id).loadNext(lastMsgId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> = messages?.toMutableList() ?: mutableListOf()
                    continuation.resume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun sendMessage(channel: Channel, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?> {
        return suspendCancellableCoroutine { continuation ->
            var tmpMessage: Message? = null
            tmpMessage = channel.sendMessage(message, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message, data = tmpMessage?.toSceytUiMessage()))
                }
            })
            tmpMessageCb.invoke(tmpMessage)
        }
    }

    override suspend fun deleteMessage(channel: Channel, message: Message): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.deleteMessage(message, true, object : MessageCallback {
                override fun onResult(msg: Message) {
                    continuation.resume(SceytResponse.Success(msg.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.resume(SceytResponse.Error(ex?.message, message.toSceytUiMessage()))
                }
            })
        }
    }

    override suspend fun editMessage(channel: Channel, message: Message): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.editMessage(message, object : MessageCallback {
                override fun onResult(p0: Message?) {
                    continuation.resume(SceytResponse.Success(message.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.resume(SceytResponse.Error(ex?.message))
                }
            })
        }
    }

    override suspend fun addReaction(channel: Channel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.addReactionWithMessageId(messageId, scoreKey, 1, "", false, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun deleteReaction(channel: Channel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.deleteReactionWithMessageId(messageId, scoreKey, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun markAsRead(channel: Channel, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            channel.markMessagesAsRead(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.resume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun markAllAsRead(channel: Channel): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            channel.markAllMessagesAsRead(object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.resume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun join(channel: Channel): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            (channel as PublicChannel).join(object : ChannelCallback {
                override fun onResult(result: Channel) {
                    continuation.resume(SceytResponse.Success(result.toSceytUiChannel()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun sendTypingState(channel: Channel, typing: Boolean) {
        if (typing)
            channel.startTyping()
        else channel.stopTyping()
    }
}