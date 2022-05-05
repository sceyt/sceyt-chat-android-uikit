package com.sceyt.chat.ui.presentation.uicomponents.channels.listeners

import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem

class ChannelsListenersImpl : ChannelListeners.Listeners {

    private var channelClickListener: ChannelListeners.ChannelClickListener? = null
    private var channelLongClickListener: ChannelListeners.ChannelLongClickListener? = null
    private var avatarClickListener: ChannelListeners.AvatarClickListener? = null

    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        channelClickListener?.onChannelClick(item)
    }

    override fun onChannelLongClick(item: ChannelListItem.ChannelItem) {
        channelLongClickListener?.onChannelLongClick(item)
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        avatarClickListener?.onAvatarClick(item) ?: run {
            channelClickListener?.onChannelClick(item)
        }
    }

    fun setListener(listener: ChannelListeners) {
        when (listener) {
            is ChannelListeners.Listeners -> {
                channelClickListener = listener
                channelLongClickListener = listener
                avatarClickListener = listener
            }
            is ChannelListeners.ChannelClickListener -> {
                channelClickListener = listener
            }
            is ChannelListeners.ChannelLongClickListener -> {
                channelLongClickListener = listener
            }
            is ChannelListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }
        }
    }
}