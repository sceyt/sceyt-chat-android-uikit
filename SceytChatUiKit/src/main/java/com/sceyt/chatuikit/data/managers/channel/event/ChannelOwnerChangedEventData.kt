package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember

data class ChannelOwnerChangedEventData(
        val channel: SceytChannel,
        var newOwner: SceytMember,
        val oldOwner: SceytMember
)