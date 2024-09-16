package com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListView

open class ChannelPopupClickListenersImpl(view: ChannelListView) : ChannelPopupClickListeners.PopupClickListeners {
    private var defaultListeners: ChannelPopupClickListeners.PopupClickListeners = view
    private var markAsReadListener: ChannelPopupClickListeners.MarkAsRead? = null
    private var pinListener: ChannelPopupClickListeners.Pin? = null
    private var unPinListener: ChannelPopupClickListeners.UnPin? = null
    private var muteListener: ChannelPopupClickListeners.Mute? = null
    private var unMuteListener: ChannelPopupClickListeners.UnMute? = null
    private var markAsUnReadListener: ChannelPopupClickListeners.MarkAsUnRead? = null
    private var leaveChannelListener: ChannelPopupClickListeners.LeaveChannel? = null
    private var deleteChannelListener: ChannelPopupClickListeners.DeleteChannel? = null
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

    override fun onPinClick(channel: SceytChannel) {
        defaultListeners.onPinClick(channel)
        pinListener?.onPinClick(channel)
    }

    override fun onUnPinClick(channel: SceytChannel) {
        defaultListeners.onUnPinClick(channel)
        unPinListener?.onUnPinClick(channel)
    }

    override fun onMuteClick(channel: SceytChannel) {
        defaultListeners.onMuteClick(channel)
        muteListener?.onMuteClick(channel)
    }

    override fun onUnMuteClick(channel: SceytChannel) {
        defaultListeners.onUnMuteClick(channel)
        unMuteListener?.onUnMuteClick(channel)
    }

    override fun onLeaveChannelClick(channel: SceytChannel) {
        defaultListeners.onLeaveChannelClick(channel)
        leaveChannelListener?.onLeaveChannelClick(channel)
    }

    override fun onDeleteChannelClick(channel: SceytChannel) {
        defaultListeners.onDeleteChannelClick(channel)
        deleteChannelListener?.onDeleteChannelClick(channel)
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
                pinListener = listener
                unPinListener = listener
                muteListener = listener
                unMuteListener = listener
                deleteChannelListener = listener
                markAsReadListener = listener
                markAsUnReadListener = listener
                leaveChannelListener = listener
                clearHistoryListener = listener
                blockChannelListener = listener
                blockUserListener = listener
                unBlockUserListener = listener
            }

            is ChannelPopupClickListeners.Pin -> {
                pinListener = listener
            }

            is ChannelPopupClickListeners.UnPin -> {
                unPinListener = listener
            }

            is ChannelPopupClickListeners.Mute -> {
                muteListener = listener
            }

            is ChannelPopupClickListeners.UnMute -> {
                unMuteListener = listener
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

            is ChannelPopupClickListeners.DeleteChannel -> {
                deleteChannelListener = listener
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