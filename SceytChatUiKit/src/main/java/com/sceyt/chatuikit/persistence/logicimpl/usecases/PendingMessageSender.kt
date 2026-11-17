package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb

internal fun interface PendingMessageSender {
    suspend fun send(message: MessageDb, isUploadedForward: Boolean)
}
