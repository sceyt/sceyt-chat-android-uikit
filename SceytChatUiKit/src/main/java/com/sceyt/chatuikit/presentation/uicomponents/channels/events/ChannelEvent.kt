package com.sceyt.chatuikit.presentation.uicomponents.channels.events

import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed class ChannelEvent {
    data class Pin(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class UnPin(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class Mute(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class UnMute(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class MarkAsRead(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class MarkAsUnRead(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class ClearHistory(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class DeleteChannel(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class LeaveChannel(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class BlockChannel(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class BlockUser(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class UnBlockUser(
            val channel: SceytChannel
    ) : ChannelEvent()
}