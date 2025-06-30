package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.ActiveUser

data class UserActivityTitleFormatterAttributes(
        val channel: SceytChannel,
        val activeUsers: List<ActiveUser>,
)