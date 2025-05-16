package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser

data class TypingTitleFormatterAttributes(
        val channel: SceytChannel,
        val users: List<SceytUser>,
)