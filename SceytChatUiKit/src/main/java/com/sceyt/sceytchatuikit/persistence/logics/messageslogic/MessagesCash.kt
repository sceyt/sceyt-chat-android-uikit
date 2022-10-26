package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

class MessagesCash {
    private var cashedMessages = hashMapOf<Long, SceytMessage>()
    private val syncOb = Any()


    fun addAll(list: List<SceytMessage>) {
        synchronized(syncOb) {
            cashedMessages.putAll(list.associateBy { it.tid })
        }
    }

    fun add(message: SceytMessage) {
        synchronized(syncOb) {
            cashedMessages[message.id] = message
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
                cashedMessages[it.id] = it
            }
        }
    }
}