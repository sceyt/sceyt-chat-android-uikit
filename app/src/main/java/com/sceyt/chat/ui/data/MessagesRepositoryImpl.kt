package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessagesListQuery
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chat.sceyt_callbacks.MessageMarkCallback
import com.sceyt.chat.sceyt_callbacks.MessagesCallback
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventsObserverService
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessagesRepositoryImpl(conversationId: Long,
                             private val channel: Channel,
                             private val replayInThread: Boolean) {
    //todo need to add DI
    // private val channelEventsService = ChannelEventsObserverService()

    val onMessageFlow = ChannelEventsObserverService.onMessageCleanFlow
        .filter { it?.first?.id == channel.id || it?.second?.replyInThread != replayInThread }
        .mapNotNull { it?.second?.toSceytUiMessage() }

    val onMessageStatusFlow = ChannelEventsObserverService.onMessageStatusFlow
        .filter { it?.channel?.id == channel.id }

    val onMessageReactionUpdatedFlow = ChannelEventsObserverService.onMessageReactionUpdatedChannel.consumeAsFlow()
        .filterNotNull()
        .filter { it.channelId == channel.id || it.replyInThread != replayInThread }

    val onMessageEditedOrDeleteFlow = ChannelEventsObserverService.onMessageEditedOrDeletedChannel.consumeAsFlow()
        .filterNotNull()
        .filter { it.channelId == channel.id || it.replyInThread != replayInThread }

    val onChannelEventFlow = ChannelEventsObserverService.onChannelEventChannel.consumeAsFlow()
        .filter { it.channelId == channel.id }

    private val query = MessagesListQuery.Builder(conversationId).apply {
        setIsThread(replayInThread)
    }.build()


    suspend fun getMessages(lastMessageId: Long): SceytResponse<List<SceytMessage>> {
        return getMessagesCoroutine(lastMessageId)
    }

    private suspend fun getMessagesCoroutine(lastMessageId: Long): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            query.setLimit(MESSAGES_LOAD_SIZE)
            query.loadPrev(lastMessageId, object : MessagesCallback {
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

    suspend fun sendMessage(message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?> {
        return suspendCancellableCoroutine { continuation ->
            val tmpMessage = channel.sendMessage(message, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message, data = message.toSceytUiMessage()))
                }
            })
            tmpMessageCb.invoke(tmpMessage)
        }
    }


    suspend fun deleteMessage(message: Message): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.deleteMessage(message, object : MessageCallback {
                override fun onResult(p0: Message?) {
                    continuation.resume(SceytResponse.Success(message.toSceytUiMessage()))
                }

                override fun onError(ex: SceytException?) {
                    continuation.resume(SceytResponse.Error(ex?.message))
                }
            })
        }
    }

    suspend fun editMessage(message: Message): SceytResponse<SceytMessage> {
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

    suspend fun addReaction(messageId: Long, score: ReactionScore): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.addReactionWithMessageId(messageId, score.key, score.score.toShort(), "", false, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    suspend fun deleteReaction(messageId: Long, score: ReactionScore): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            channel.deleteReactionWithMessageId(messageId, score.key, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.resume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }

    suspend fun markAsRead(id: Long): SceytResponse<MessageListMarker> {
        return suspendCancellableCoroutine { continuation ->
            channel.markMessagesAsRead(longArrayOf(id), object : MessageMarkCallback {
                override fun onResult(result: MessageListMarker) {
                    continuation.resume(SceytResponse.Success(result))
                }

                override fun onError(error: SceytException?) {
                    continuation.resume(SceytResponse.Error(error?.message))
                }
            })
        }
    }
}