package com.sceyt.chatuikit.data.models

import com.sceyt.chatuikit.data.models.messages.SceytMessage

data class SyncNearMessagesResult(
        val centerMessageId: Long,
        val response: SceytResponse<List<SceytMessage>>,
        val missingMessages: List<SceytMessage>,
)