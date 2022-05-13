package com.sceyt.chat.ui.presentation.uicomponents.channels.listeners

import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem

class ChannelsClickListenersImpl : ChannelClickListeners.ClickListeners {

    private var channelClickListener: ChannelClickListeners.ChannelClickClickListener? = null
    private var channelLongClickListener: ChannelClickListeners.ChannelClickLongClickListener? = null
    private var avatarClickListener: ChannelClickListeners.AvatarClickListener? = null

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