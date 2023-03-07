package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel

data class ChannelUpdateData(
        val channel: SceytChannel,
        val needSorting: Boolean
)