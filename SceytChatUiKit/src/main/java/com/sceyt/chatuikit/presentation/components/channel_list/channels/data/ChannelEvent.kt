package com.sceyt.chatuikit.presentation.components.channel_list.channels.data

import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed class ChannelEvent {
    data class Pin(
            val channel: SceytChannel,
    ) : ChannelEvent()

    data class UnPin(
            val channel: SceytChannel,
    ) : ChannelEvent()

    data class Mute(
            val channel: SceytChannel,
            val muteUntil: Long,
    ) : ChannelEvent()

    data class UnMute(
            val channel: SceytChannel,
    ) : ChannelEvent()

    data class MarkAsRead(
            val channel: SceytChannel,
    ) : ChannelEvent()

    data class MarkAsUnRead(
            val channel: SceytChannel,
    ) : ChannelEvent()

    data class ClearHistory(
            val channel: SceytChannel,
    ) : ChannelEvent()

    data class DeleteChannel(
            val channel: SceytChannel,
    ) : ChannelEvent()

    data class LeaveChannel(
            val channel: SceytChannel,
    ) : ChannelEvent()
}