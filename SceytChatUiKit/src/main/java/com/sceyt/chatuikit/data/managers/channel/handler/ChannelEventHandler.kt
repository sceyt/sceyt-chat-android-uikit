package com.sceyt.chatuikit.data.managers.channel.handler

import com.sceyt.chat.models.member.Member
import com.sceyt.chatuikit.data.managers.channel.event.ChannelEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed interface ChannelEventHandler {

    fun interface OnTotalUnreadChanged : ChannelEventHandler {
        fun onTotalUnreadChanged(data: ChannelUnreadCountUpdatedEventData)
    }

    fun interface OnChannelEvent : ChannelEventHandler {
        fun onChannelEvent(data: ChannelEventData)
    }

    fun interface OnOwnerChanged : ChannelEventHandler {
        fun onOwnerChanged(channel: SceytChannel, newOwner: Member, oldOwner: Member)
    }

    fun interface OnChannelTypingEvent : ChannelEventHandler {
        fun onChannelTypingEvent(data: ChannelTypingEventData)
    }

    fun interface OnChangedMembersEvent : ChannelEventHandler {
        fun onChangedMembersEvent(data: ChannelMembersEventData)
    }

    fun interface OnMessageStatusEvent : ChannelEventHandler {
        fun onMessageStatusEvent(data: MessageStatusChangeData)
    }

    fun interface OnMarkerReceivedEvent : ChannelEventHandler {
        fun onMarkerReceived(data: MessageMarkerEventData)
    }

    interface AllEvents : OnTotalUnreadChanged, OnChannelEvent, OnOwnerChanged,
            OnChannelTypingEvent, OnChangedMembersEvent, OnMessageStatusEvent, OnMarkerReceivedEvent
}