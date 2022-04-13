package com.sceyt.chat.ui

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.data.models.SceytUiDirectChannel
import com.sceyt.chat.ui.data.models.SceytUiGroupChannel
import com.sceyt.chat.ui.data.models.getChannelType


fun Channel.toSceytUiChannel(): SceytUiChannel {
    if (this is GroupChannel)
        return SceytUiGroupChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            lastMessage = lastMessage,
            label = label,
            metadata = metadata,
            muted = muted(),
            muteExpireDate = muteExpireDate(),
            channelType = getChannelType(this),
            subject = subject,
            avatarUrl = avatarUrl,
            members = members,
            memberCount = memberCount
        )
    else {
        this as DirectChannel
        return SceytUiDirectChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            lastMessage = lastMessage,
            label = label,
            metadata = metadata,
            muted = muted(),
            peer = peer,
            channelType = getChannelType(this),
        )
    }
}