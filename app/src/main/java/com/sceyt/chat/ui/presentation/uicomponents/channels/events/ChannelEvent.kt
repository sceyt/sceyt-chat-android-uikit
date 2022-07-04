package com.sceyt.chat.ui.presentation.uicomponents.channels.events

import com.sceyt.chat.ui.data.models.channels.SceytChannel

sealed class ChannelEvent {
    data class MarkAsRead(
            val channel: SceytChannel
    ) : ChannelEvent()

    data class ClearHistory(
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