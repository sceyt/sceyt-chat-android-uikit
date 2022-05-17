package com.sceyt.chat.ui.data

import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessagesListQuery
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.sceyt_callbacks.MessagesCallback
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessagesRepositoryImpl(private val channelId: Long,
                             isThread: Boolean) {
    private val observer = ChannelEventsObserverService()

    val onMessageFlow = observer.onMessageFlow.filter {
        it?.first?.id == channelId
    }.mapNotNull {
        it?.second?.toSceytUiMessage()
    }

    val onMessageStatusFlow = observer.onMessageStatusFlow.filter {
        it?.channel?.id == channelId
    }

    private val query = MessagesListQuery.Builder(channelId).apply {
        setIsThread(isThread)
    }.build()


    suspend fun getMessages(lastMessageId: Long): SceytResponse<List<SceytUiMessage>> {
        return getMessagesCoroutine(lastMessageId)
    }

    private suspend fun getMessagesCoroutine(lastMessageId: Long): SceytResponse<List<SceytUiMessage>> {
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

    suspend fun addReaction(message: SceytUiMessage, score: ReactionScore): SceytResponse<SceytUiMessage> {
        return suspendCancellableCoroutine { continuation ->
            ClientWrapper.addReaction(channelId, message.id, score.key, score.score.toInt(), "", false) { message, status ->
                if (status == null || status.isOk) {
                    continuation.resume(SceytResponse.Success(message.toSceytUiMessage()))
                } else {
                    continuation.resume(SceytResponse.Error(status.error?.message))
                }
            }
        }
    }

    suspend fun sendMessage(message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytUiMessage> {
        return suspendCancellableCoroutine { continuation ->
            val tmpMessage = ClientWrapper.sendMessage(channelId, message) { message, status ->
                if (status == null || status.isOk) {
                    continuation.resume(SceytResponse.Success(message.toSceytUiMessage()))
                } else {
                    continuation.resume(SceytResponse.Error(status.error?.message))
                }
            }
            tmpMessageCb.invoke(tmpMessage)
        }
    }
}