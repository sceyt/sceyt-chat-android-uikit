package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners

import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel

open class ChannelPopupClickListenersImpl(view: ChannelsListView) : ChannelPopupClickListeners.PopupClickListeners {
    private var defaultListeners: ChannelPopupClickListeners.PopupClickListeners = view
    private var markAsReadListener: ChannelPopupClickListeners.MarkAsRead? = null
    private var markAsUnReadListener: ChannelPopupClickListeners.MarkAsUnRead? = null
    private var leaveChannelListener: ChannelPopupClickListeners.LeaveChannel? = null
    private var clearHistoryListener: ChannelPopupClickListeners.ClearHistory? = null
    private var blockChannelListener: ChannelPopupClickListeners.BlockChannel? = null
    private var blockUserListener: ChannelPopupClickListeners.BlockUser? = null
    private var unBlockUserListener: ChannelPopupClickListeners.UnBlockUser? = null

    override fun onMarkAsReadClick(channel: SceytChannel) {
        defaultListeners.onMarkAsReadClick(channel)
        markAsReadListener?.onMarkAsReadClick(channel)
    }

    override fun onMarkAsUnReadClick(channel: SceytChannel) {
        defaultListeners.onMarkAsUnReadClick(channel)
        markAsUnReadListener?.onMarkAsUnReadClick(channel)
    }

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

    override fun onBlockUserClick(channel: SceytChannel) {
        defaultListeners.onBlockUserClick(channel)
        blockUserListener?.onBlockUserClick(channel)
    }

    override fun onUnBlockUserClick(channel: SceytChannel) {
        defaultListeners.onUnBlockUserClick(channel)
        unBlockUserListener?.onUnBlockUserClick(channel)
    }

    fun setListener(listener: ChannelPopupClickListeners) {
        when (listener) {
            is ChannelPopupClickListeners.PopupClickListeners -> {
                markAsReadListener = listener
                markAsUnReadListener = listener
                leaveChannelListener = listener
                clearHistoryListener = listener
                blockChannelListener = listener
                blockUserListener = listener
                unBlockUserListener = listener
            }
            is ChannelPopupClickListeners.MarkAsRead -> {
                markAsReadListener = listener
            }
            is ChannelPopupClickListeners.MarkAsUnRead -> {
                markAsUnReadListener = listener
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
            is ChannelPopupClickListeners.BlockUser -> {
                blockUserListener = listener
            }
            is ChannelPopupClickListeners.UnBlockUser -> {
                unBlockUserListener = listener
            }
        }
    }
}