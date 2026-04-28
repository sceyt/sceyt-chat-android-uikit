package com.sceyt.chatuikit.data.models.search

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage

data class GlobalSearchMessageResult(
    val message: SceytMessage,
    val channel: SceytChannel,
)
