package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessagesListQuery
import com.sceyt.chat.sceyt_callbacks.MessagesCallback
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MessagesRepositoryImpl(channelId: Long,
                             isThread: Boolean) {

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
}