package com.sceyt.chat.ui.presentation.uicomponents.channels.listeners

import com.sceyt.chat.ui.data.models.channels.SceytChannel

sealed interface ChannelPopupClickListeners {

    fun interface MarkAsRead : ChannelPopupClickListeners {
        fun onMarkAsReadClick(channel: SceytChannel)
    }

    fun interface LeaveChannel : ChannelPopupClickListeners {
        fun onLeaveChannelClick(channel: SceytChannel)
    }

    fun interface ClearHistory : ChannelPopupClickListeners {
        fun onClearHistoryClick(channel: SceytChannel)
    }

    fun interface BlockChannel : ChannelPopupClickListeners {
        fun onBlockChannelClick(channel: SceytChannel)
    }

    fun interface BlockUser : ChannelPopupClickListeners {
        fun onBlockUserClick(channel: SceytChannel)
    }

    /** User this if you want to implement all callbacks */
    interface PopupClickListeners : LeaveChannel, ClearHistory,
            BlockChannel, MarkAsRead, BlockUser
}