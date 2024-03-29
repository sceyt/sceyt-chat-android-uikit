package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.message.Message

open class MessageTransformer {

    open fun transformToSend(message: Message): Message {
        return message
    }

    open fun transformToGet(message: Message): Message {
        return message
    }
}