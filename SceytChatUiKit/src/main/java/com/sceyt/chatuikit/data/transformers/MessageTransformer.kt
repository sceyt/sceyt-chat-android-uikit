package com.sceyt.chatuikit.data.transformers

import com.sceyt.chat.models.message.Message

open class MessageTransformer {

    open fun transformToSend(message: Message): Message? {
        return message
    }

    open fun transformToGet(message: Message): Message {
        return message
    }
}