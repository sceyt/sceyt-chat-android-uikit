package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators.MessageComparator
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.diffContent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MessagesCash {
    private var cashedMessages = hashMapOf<Long, SceytMessage>()
    private val syncOb = Any()

    companion object {
        private val messageUpdatedFlow_ = MutableSharedFlow<List<SceytMessage>>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messageUpdatedFlow: SharedFlow<List<SceytMessage>> = messageUpdatedFlow_
    }

    /** Added messages like upsert, and check is differences between messages*/
    fun addAll(list: List<SceytMessage>, checkDifference: Boolean): Boolean {
        synchronized(syncOb) {
            return if (checkDifference)
                putAndCheckHasDiff(list)
            else {
                cashedMessages.putAll(list.associateBy { it.tid })
                false
            }
        }
    }

    fun add(message: SceytMessage) {
        synchronized(syncOb) {
            cashedMessages[message.tid] = message
            emitMessageUpdated(message)
        }
    }

    fun clear() {
        synchronized(syncOb) {
            cashedMessages.clear()
        }
    }

    fun getSorted(): List<SceytMessage> {
        synchronized(syncOb) {
            return cashedMessages.values.sortedWith(MessageComparator()).map { it.clone() }
        }
    }

    fun messageUpdated(vararg message: SceytMessage) {
        synchronized(syncOb) {
            message.forEach {
                cashedMessages[it.tid] = it
            }
            emitMessageUpdated(*message)
        }
    }

    fun updateMessagesStatus(status: DeliveryStatus, vararg tIds: Long) {
        synchronized(syncOb) {
            tIds.forEach {
                cashedMessages[it]?.let { message ->
                    message.deliveryStatus = status
                    emitMessageUpdated(message)
                }
            }
        }
    }

    fun deleteMessage(tid: Long) {
        synchronized(syncOb) {
            cashedMessages.remove(tid)
        }
    }

    private fun emitMessageUpdated(vararg message: SceytMessage) {
        messageUpdatedFlow_.tryEmit(message.map { it.clone() })
    }

    private fun putAndCheckHasDiff(list: List<SceytMessage>): Boolean {
        var detectedDiff = false
        list.forEach {
            if (!detectedDiff) {
                val old = cashedMessages[it.tid]
                detectedDiff = old?.diffContent(it)?.hasDifference() ?: true
            }
            cashedMessages[it.tid] = it
        }
        return detectedDiff
    }
}