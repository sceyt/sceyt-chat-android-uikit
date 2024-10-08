package com.sceyt.chatuikit.data.managers.channel.handler

import com.sceyt.chat.models.member.Member
import com.sceyt.chatuikit.data.managers.channel.event.ChannelEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.channels.SceytChannel

class ChannelEventHandlerImpl : ChannelEventHandler.AllEvents {
    private var defaultListeners: ChannelEventHandler.AllEvents? = null

    constructor()

    internal constructor(defaultListener: ChannelEventHandler.AllEvents) : this() {
        defaultListeners = defaultListener
    }

    override fun onTotalUnreadChanged(data: ChannelUnreadCountUpdatedEventData) {
        defaultListeners?.onTotalUnreadChanged(data)
    }

    override fun onChannelEvent(data: ChannelEventData) {
        defaultListeners?.onChannelEvent(data)
    }

    override fun onOwnerChanged(channel: SceytChannel, newOwner: Member, oldOwner: Member) {
        defaultListeners?.onOwnerChanged(channel, newOwner, oldOwner)
    }

    override fun onChannelTypingEvent(data: ChannelTypingEventData) {
        defaultListeners?.onChannelTypingEvent(data)
    }

    override fun onChangedMembersEvent(data: ChannelMembersEventData) {
        defaultListeners?.onChangedMembersEvent(data)
    }

    override fun onMessageStatusEvent(data: MessageStatusChangeData) {
        defaultListeners?.onMessageStatusEvent(data)
    }

    override fun onMarkerReceived(data: MessageMarkerEventData) {
        defaultListeners?.onMarkerReceived(data)
    }

    internal fun setDefaultListeners(listener: ChannelEventHandler.AllEvents) {
        defaultListeners = listener
    }
}