package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.differs.ChannelDiff

data class ChannelUpdateData(
        var channel: SceytChannel,
        val needSorting: Boolean,
        val diff: ChannelDiff,
        val eventType: ChannelUpdatedType = ChannelUpdatedType.Updated
)

enum class ChannelUpdatedType {
    Updated, Presence, LastMessage, ClearedHistory, MuteState, Members, UnreadCount, PinnedAt, AutoDeleteState
}