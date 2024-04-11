package com.sceyt.chatuikit.data.channeleventobserver

import com.sceyt.chat.models.member.Member
import com.sceyt.chatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed interface ChannelEventManager {

    fun interface OnTotalUnreadChanged : ChannelEventManager {
        fun onTotalUnreadChanged(data: ChannelUnreadCountUpdatedEventData)
    }

    fun interface OnChannelEvent : ChannelEventManager {
        fun onChannelEvent(data: ChannelEventData)
    }

    fun interface OnOwnerChanged : ChannelEventManager {
        fun onOwnerChanged(channel: SceytChannel, newOwner: Member, oldOwner: Member)
    }

    fun interface OnChannelTypingEvent : ChannelEventManager {
        fun onChannelTypingEvent(data: ChannelTypingEventData)
    }

    fun interface OnChangedMembersEvent : ChannelEventManager {
        fun onChangedMembersEvent(data: ChannelMembersEventData)
    }

    fun interface OnMessageStatusEvent : ChannelEventManager {
        fun onMessageStatusEvent(data: MessageStatusChangeData)
    }

    interface AllEventManagers : OnTotalUnreadChanged, OnChannelEvent, OnOwnerChanged,
            OnChannelTypingEvent, OnChangedMembersEvent, OnMessageStatusEvent
}