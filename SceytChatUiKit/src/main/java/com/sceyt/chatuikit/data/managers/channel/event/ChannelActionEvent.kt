package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chat.models.channel.ChannelEvent
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember

sealed class ChannelActionEvent(
        val channelId: Long
) {
    data class Created(val channel: SceytChannel) : ChannelActionEvent(channel.id)
    data class Deleted(val id: Long) : ChannelActionEvent(id)
    data class Updated(val channel: SceytChannel) : ChannelActionEvent(channel.id)
    data class ClearedHistory(val channel: SceytChannel) : ChannelActionEvent(channel.id)
    data class Joined(
            val channel: SceytChannel,
            val joinedMembers: List<SceytMember>
    ) : ChannelActionEvent(channel.id)

    data class Left(
            val channel: SceytChannel,
            val leftMembers: List<SceytMember>
    ) : ChannelActionEvent(channel.id)

    data class Mute(
            val channel: SceytChannel,
            val muted: Boolean
    ) : ChannelActionEvent(channel.id)

    data class Pin(
            val channel: SceytChannel,
            val pinned: Boolean
    ) : ChannelActionEvent(channel.id)

    data class Hide(
            val channel: SceytChannel,
            val hidden: Boolean
    ) : ChannelActionEvent(channel.id)

    data class MarkedUs(
            val channel: SceytChannel,
            val read: Boolean
    ) : ChannelActionEvent(channel.id)

    data class Block(
            val id: Long,
            val blocked: Boolean
    ) : ChannelActionEvent(id)

    data class Event(
            val channel: SceytChannel,
            val event: ChannelEvent
    ) : ChannelActionEvent(channel.id)
}