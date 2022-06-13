package com.sceyt.chat.ui.presentation.uicomponents.channels.listeners

import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.presentation.uicomponents.channels.ChannelsListView

open class ChannelPopupClickListenersImpl(view: ChannelsListView) : ChannelPopupClickListeners.PopupClickListeners {
    private var defaultListeners: ChannelPopupClickListeners.PopupClickListeners = view
    private var leaveChannelListener: ChannelPopupClickListeners.LeaveChannel? = null
    private var clearHistoryListener: ChannelPopupClickListeners.ClearHistory? = null
    private var blockChannelListener: ChannelPopupClickListeners.BlockChannel? = null

    override fun onLeaveChannelClick(channel: SceytChannel) {
        defaultListeners.onLeaveChannelClick(channel)
        leaveChannelListener?.onLeaveChannelClick(channel)
    }

    override fun onClearHistoryClick(channel: SceytChannel) {
        defaultListeners.onClearHistoryClick(channel)
        clearHistoryListener?.onClearHistoryClick(channel)
    }

    override fun onBlockChannelClick(channel: SceytChannel) {
        defaultListeners.onBlockChannelClick(channel)
        blockChannelListener?.onBlockChannelClick(channel)
    }

    fun setListener(listener: ChannelPopupClickListeners) {
        when (listener) {
            is ChannelPopupClickListeners.PopupClickListeners -> {
                leaveChannelListener = listener
                clearHistoryListener = listener
                blockChannelListener = listener
            }
            is ChannelPopupClickListeners.LeaveChannel -> {
                leaveChannelListener = listener
            }
            is ChannelPopupClickListeners.ClearHistory -> {
                clearHistoryListener = listener
            }
            is ChannelPopupClickListeners.BlockChannel -> {
                blockChannelListener = listener
            }
        }
    }
}