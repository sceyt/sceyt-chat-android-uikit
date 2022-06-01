package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessagesListQuery
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chat.sceyt_callbacks.MessagesCallback
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessagesRepositoryImpl(private val channel: Channel,
                             isThread: Boolean) {
    //todo need to add DI
    private val channelEventsService = ChannelEventsObserverService()

    val onMessageFlow = channelEventsService.onMessageFlow
        .filter { it?.first?.id == channel.id }
        .mapNotNull { it?.second?.toSceytUiMessage() }

    val onMessageStatusFlow = channelEventsService.onMessageStatusFlow
        .filter { it?.channel?.id == channel.id }

    val onMessageReactionUpdatedFlow = channelEventsService.onMessageReactionUpdatedChannel.consumeAsFlow()
        .filterNotNull()
        .filter { it.channelId == channel.id }

    val onMessageEditedOrDeleteFlow = channelEventsService.onMessageEditedOrDeletedChannel.consumeAsFlow()
        .filterNotNull()
        .filter { it.channelId == channel.id }

    val onChannelClearedHistoryFlow = channelEventsService.onChannelClearedHistoryChannel.consumeAsFlow()
        .filterNotNull()
        .filter { it.id == channel.id }

    private val query = MessagesListQuery.Builder(channel.id).apply {
        setIsThread(isThread)
    }.build()


    suspend fun getMessages(lastMessageId: Long): SceytResponse<List<SceytMessage>> {
        return getMessagesCoroutine(lastMessageId)
    }

    private suspend fun getMessagesCoroutine(lastMessageId: Long): SceytResponse<List<SceytMessage>> {
        return suspendCancellableCoroutine { continuation ->
            query.setLimit(MESSAGES_LOAD_SIZE)
            query.loadPrev(lastMessageId, object : MessagesCallback {
                override fun onResult(messages: MutableList<Message>?) {
                    val result: MutableList<Message> =
                            messages?.toMutableList() ?: mutableListOf()
                    /*if (lastMessage != null) {
                        result.add(lastMessage)
                    }*/
                    continuation.resume(SceytResponse.Success(result.map { it.toSceytUiMessage() }))
                }

                override fun onError(e: SceytException?) {
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
}