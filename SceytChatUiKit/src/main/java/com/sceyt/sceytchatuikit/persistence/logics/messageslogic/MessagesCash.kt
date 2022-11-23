package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.common.diffContent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators.MessageComparator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MessagesCash {
    private var cashedMessages = hashMapOf<Long, SceytMessage>()
    private val lock = Any()

    companion object {
        private val messageUpdatedFlow_ = MutableSharedFlow<List<SceytMessage>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messageUpdatedFlow: SharedFlow<List<SceytMessage>> = messageUpdatedFlow_
    }

    /** Added messages like upsert, and check is differences between messages*/
    fun addAll(list: List<SceytMessage>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(true, *list.toTypedArray())
            else {
                cashedMessages.putAll(list.associateBy { it.tid })
                false
            }
        }
    }

    fun add(message: SceytMessage) {
        synchronized(lock) {
            cashedMessages[message.tid] = message
            emitMessageUpdated(message)
        }
    }

    fun clear() {
        synchronized(lock) {
            cashedMessages.clear()
        }
    }

    fun getSorted(): List<SceytMessage> {
        synchronized(lock) {
            return cashedMessages.values.sortedWith(MessageComparator()).map { it.clone() }
        }
    }

    fun messageUpdated(vararg message: SceytMessage) {
        synchronized(lock) {
            message.forEach {
                cashedMessages[it.tid] = it
            }
            emitMessageUpdated(*message)
        }
    }

    fun updateMessagesStatus(status: DeliveryStatus, vararg tIds: Long) {
        synchronized(lock) {
            tIds.forEach {
                cashedMessages[it]?.let { message ->
                    message.deliveryStatus = status
                    emitMessageUpdated(message)
                }
            }
        }
    }

    fun deleteMessage(tid: Long) {
        synchronized(lock) {
            cashedMessages.remove(tid)
        }
    }

    fun upsertMessages(vararg message: SceytMessage) {
        message.forEach {
            if (putAndCheckHasDiff(false, it))
                emitMessageUpdated(it)
        }
    }

    private fun emitMessageUpdated(vararg message: SceytMessage) {
        messageUpdatedFlow_.tryEmit(message.map { it.clone() })
    }


    private fun putAndCheckHasDiff(includeNotExistToDiff: Boolean, vararg messages: SceytMessage): Boolean {
        var detectedDiff = false
        messages.forEach {
            if (!detectedDiff) {
                val old = cashedMessages[it.tid]
                detectedDiff = old?.diffContent(it)?.hasDifference() ?: includeNotExistToDiff
            }
            cashedMessages[it.tid] = it
        }
        return detectedDiff
    }
}