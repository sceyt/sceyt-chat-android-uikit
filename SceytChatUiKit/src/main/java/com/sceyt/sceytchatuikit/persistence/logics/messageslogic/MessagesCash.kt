package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.diffContent

class MessagesCash {
    private var cashedMessages = hashMapOf<Long, SceytMessage>()
    private val syncOb = Any()

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
        }
    }

    fun clear() {
        synchronized(syncOb) {
            cashedMessages.clear()
        }
    }

    fun get(): List<SceytMessage> {
        synchronized(syncOb) {
            return cashedMessages.values.sortedBy { it.createdAt }.map { it.clone() }
        }
    }

    fun updateMessage(vararg message: SceytMessage) {
        synchronized(syncOb) {
            message.forEach {
                cashedMessages[it.tid] = it
            }
        }
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