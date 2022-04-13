package com.sceyt.chat.ui.presentation.channels.listeners

import com.sceyt.chat.models.channel.Channel

interface ChannelListListener {
    fun channelClickListener(channel: Channel)
    fun channelLongClickListener(channel: Channel)
    fun deleteClickListener(channel: Channel)
    fun avatarClickListener(channel: Channel)
}