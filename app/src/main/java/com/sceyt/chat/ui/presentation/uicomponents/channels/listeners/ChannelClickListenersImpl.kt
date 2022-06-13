package com.sceyt.chat.ui.presentation.uicomponents.channels.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem

open class ChannelClickListenersImpl : ChannelClickListeners.ClickListeners {
    private var defaultListeners: ChannelClickListeners.ClickListeners? = null
    private var channelClickListener: ChannelClickListeners.ChannelClickClickListener? = null
    private var channelLongClickListener: ChannelClickListeners.ChannelClickLongClickListener? = null
    private var avatarClickListener: ChannelClickListeners.AvatarClickListener? = null

    internal constructor()

    constructor(view: ChannelsListView) {
        defaultListeners = view
    }

    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        defaultListeners?.onChannelClick(item)
        channelClickListener?.onChannelClick(item)
    }

    override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
        defaultListeners?.onChannelLongClick(view, item)
        channelLongClickListener?.onChannelLongClick(view, item)
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        defaultListeners?.onAvatarClick(item)
        avatarClickListener?.onAvatarClick(item)
    }

    fun setListener(listener: ChannelClickListeners) {
        when (listener) {
            is ChannelClickListeners.ClickListeners -> {
                channelClickListener = listener
                channelLongClickListener = listener
                avatarClickListener = listener
            }
            is ChannelClickListeners.ChannelClickClickListener -> {
                channelClickListener = listener
            }
            is ChannelClickListeners.ChannelClickLongClickListener -> {
                channelLongClickListener = listener
            }
            is ChannelClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }
        }
    }
}