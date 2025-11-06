package com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem

open class ChannelClickListenersImpl : ChannelClickListeners.ClickListeners {
    private var defaultListeners: ChannelClickListeners.ClickListeners? = null
    private var channelClickListener: ChannelClickListeners.ChannelClickListener? = null
    private var channelLongClickListener: ChannelClickListeners.ChannelLongClickListener? = null
    private var avatarClickListener: ChannelClickListeners.AvatarClickListener? = null

    constructor()

    internal constructor(listeners: ChannelClickListeners.ClickListeners) {
        defaultListeners = listeners
    }

    override fun onChannelClick(view: View, item: ChannelListItem.ChannelItem) {
        defaultListeners?.onChannelClick(view, item)
        channelClickListener?.onChannelClick(view, item)
    }

    override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
        defaultListeners?.onChannelLongClick(view, item)
        channelLongClickListener?.onChannelLongClick(view, item)
    }

    override fun onAvatarClick(view: View, item: ChannelListItem.ChannelItem) {
        defaultListeners?.onAvatarClick(view, item)
        avatarClickListener?.onAvatarClick(view, item)
    }

    fun setListener(listener: ChannelClickListeners) {
        when (listener) {
            is ChannelClickListeners.ClickListeners -> {
                channelClickListener = listener
                channelLongClickListener = listener
                avatarClickListener = listener
            }

            is ChannelClickListeners.ChannelClickListener -> {
                channelClickListener = listener
            }

            is ChannelClickListeners.ChannelLongClickListener -> {
                channelLongClickListener = listener
            }

            is ChannelClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listeners: ChannelClickListeners.ClickListeners,
    ): ChannelClickListenersImpl {
        defaultListeners = listeners
        return this
    }
}