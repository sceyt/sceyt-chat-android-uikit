package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import org.koin.core.component.inject

object ChatReactionMessagesCache : SceytKoinComponent {

    private val messageCash: MessagesCache by inject()
    private val list = mutableMapOf<Long, SceytMessage>()

    suspend fun getNeededMessages(ids: Map<Long, Long>) {
        ids.forEach {
            getMessage(it.key, it.value)
        }
    }

    private suspend fun getMessage(channelId: Long, messageId: Long) {
        messageCash.get(messageId)?.let {
            list[it.id] = it
        } ?: run {
            SceytKitClient.getMessagesMiddleWare().getMessageDbById(messageId)?.let {
                list[it.id] = it
            } ?: run {
                val result = SceytKitClient.getMessagesMiddleWare().getMessageFromServerById(channelId, messageId)
                if (result is SceytResponse.Success) {
                    result.data?.let { list[it.id] = it }
                }
            }
        }
    }

    fun addMessage(message: SceytMessage) {
        list[message.id] = message
    }

    fun getMessageById(messageId: Long): SceytMessage? {
        return list[messageId]
    }
}