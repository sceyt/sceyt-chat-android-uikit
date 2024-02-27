package com.sceyt.sceytchatuikit.data.models

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

data class SyncNearMessagesResult(
        val centerMessageId: Long,
        val response: SceytResponse<List<SceytMessage>>,
        val missingMessages: List<SceytMessage>,
)