package com.sceyt.sceytchatuikit.data.models

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed class SendMessageResult {
    data class TempMessage(val message: SceytMessage) : SendMessageResult()
    object StartedSendingAttachment : SendMessageResult()
    data class Response(val response: SceytResponse<SceytMessage>) : SendMessageResult()
}