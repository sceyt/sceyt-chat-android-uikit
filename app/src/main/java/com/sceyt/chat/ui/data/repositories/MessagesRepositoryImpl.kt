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
import com.sceyt.chat.ui.data.toChannel
import com.sceyt.chat.ui.data.toMessage
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
    override suspend fun getMessages(channel: SceytChannel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): SceytResponse<List<SceytMessage>> {
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
    override suspend fun getMessagesByType(channel: SceytChannel, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>> {
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

    override suspend fun sendMessage(channel: SceytChannel, message: Message, tmpMessageCb:  (Message) -> Unit): SceytResponse<SceytMessage?> {
        return suspendCancellableCoroutine { continuation ->
            var tmpMessage: Message? = null
            tmpMessage = channel.toChannel().sendMessage(message, object : MessageCallback {
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

    override suspend fun deleteMessage(channel: SceytChannel, messageId: Long): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.toChannel().deleteMessage(messageId, true, object : MessageCallback {
                override fun onResult(msg: Message) {
                    continuation.resume(SceytResponse.Success(msg.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.resume(SceytResponse.Error(ex?.message))
                }
            })
        }
    }

    override suspend fun editMessage(channel: SceytChannel, message: SceytMessage): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.toChannel().editMessage(message.toMessage(), object : MessageCallback {
                override fun onResult(result: Message) {
                    continuation.resume(SceytResponse.Success(result.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.resume(SceytResponse.Error(ex?.message))
                }
            })
        }
    }

    override suspend fun addReaction(channel: SceytChannel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.toChannel().addReactionWithMessageId(messageId, scoreKey, 1, "", false, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun deleteReaction(channel: SceytChannel, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.toChannel().deleteReactionWithMessageId(messageId, scoreKey, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun markAsRead(channel: SceytChannel, vararg id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            channel.toChannel().markMessagesAsRead(id, object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.resume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun markAllAsRead(channel: SceytChannel): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            channel.toChannel().markAllMessagesAsRead(object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.resume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun join(channel: SceytChannel): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            (channel.toChannel() as PublicChannel).join(object : ChannelCallback {
                override fun onResult(result: Channel) {
                    continuation.resume(SceytResponse.Success(result.toSceytUiChannel()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    override suspend fun sendTypingState(channel: SceytChannel, typing: Boolean) {
        if (typing)
            channel.toChannel().startTyping()
        else channel.toChannel().stopTyping()
    }
}