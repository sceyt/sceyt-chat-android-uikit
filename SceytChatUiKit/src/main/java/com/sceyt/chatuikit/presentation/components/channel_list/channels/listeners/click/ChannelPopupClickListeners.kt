package com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click

import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed interface ChannelPopupClickListeners {

    fun interface Pin : ChannelPopupClickListeners {
        fun onPinClick(channel: SceytChannel)
    }

    fun interface UnPin : ChannelPopupClickListeners {
        fun onUnPinClick(channel: SceytChannel)
    }

    fun interface Mute : ChannelPopupClickListeners {
        fun onMuteClick(channel: SceytChannel)
    }

    fun interface UnMute : ChannelPopupClickListeners {
        fun onUnMuteClick(channel: SceytChannel)
    }

    fun interface MarkAsRead : ChannelPopupClickListeners {
        fun onMarkAsReadClick(channel: SceytChannel)
    }

    fun interface MarkAsUnRead : ChannelPopupClickListeners {
        fun onMarkAsUnReadClick(channel: SceytChannel)
    }

    fun interface LeaveChannel : ChannelPopupClickListeners {
        fun onLeaveChannelClick(channel: SceytChannel)
    }

    fun interface DeleteChannel : ChannelPopupClickListeners {
        fun onDeleteChannelClick(channel: SceytChannel)
    }

    fun interface ClearHistory : ChannelPopupClickListeners {
        fun onClearHistoryClick(channel: SceytChannel)
    }

    /** Use this if you want to implement all callbacks */
    interface PopupClickListeners : Pin, UnPin, Mute, UnMute, LeaveChannel, DeleteChannel,
            ClearHistory, MarkAsRead, MarkAsUnRead
}