package com.sceyt.chatuikit.data.channeleventobserver

import com.sceyt.chat.models.member.Member
import com.sceyt.chatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.channels.SceytChannel

class ChannelEventManagerImpl : ChannelEventManager.AllEventManagers {
    private var defaultListeners: ChannelEventManager.AllEventManagers? = null

    constructor()

    internal constructor(defaultListener: ChannelEventManager.AllEventManagers) : this() {
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

    internal fun setDefaultListeners(listener: ChannelEventManager.AllEventManagers) {
        defaultListeners = listener
    }
}