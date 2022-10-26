package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

class MessagesCash {
    private var cashedMessages = hashMapOf<Long, SceytMessage>()
    private val syncOb = Any()


    fun addAll(list: List<SceytMessage>) {
        synchronized(syncOb) {
            cashedMessages.putAll(list.associateBy { it.id })
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
}