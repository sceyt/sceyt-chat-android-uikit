package com.sceyt.sceytchatuikit.data.models

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed class SendMessageResult {
    data class TempMessage(val message: SceytMessage) : SendMessageResult()
    object StartedSendingAttachment : SendMessageResult()
    data class Success(val response: SceytResponse.Success<SceytMessage>) : SendMessageResult()
    data class Error(val response: SceytResponse.Error<SceytMessage>) : SendMessageResult()

    fun isServerResponse() = this is Success || this is Error

    fun response() = when (this) {
        is Success -> response
        is Error -> response
        else -> null
    }
}