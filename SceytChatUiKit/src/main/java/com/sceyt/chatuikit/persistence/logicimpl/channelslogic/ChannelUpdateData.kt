package com.sceyt.chatuikit.persistence.logicimpl.channelslogic

import com.sceyt.chatuikit.data.models.channels.SceytChannel

data class ChannelUpdateData(
        var channel: SceytChannel,
        val needSorting: Boolean,
        val eventType: ChannelUpdatedType = ChannelUpdatedType.Updated
)

enum class ChannelUpdatedType {
    Updated, Presence, LastMessage, ClearedHistory, MuteState, Members, UnreadCount, PinnedAt, AutoDeleteState
}