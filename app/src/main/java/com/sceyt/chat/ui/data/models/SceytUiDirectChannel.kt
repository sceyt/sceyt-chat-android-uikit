package com.sceyt.chat.ui.data.models

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message

class SceytUiDirectChannel(
        id: Long = 0,
        metadata: String? = null,
        label: String? = null,
        createdAt: Long,
        updatedAt: Long,
        unreadMessageCount: Long,
        lastMessage: Message?,
        muted: Boolean = false,
        channelType: ChannelTypeEnum = ChannelTypeEnum.Direct,
        var peer: Member?,
) : SceytUiChannel(id, createdAt, updatedAt, unreadMessageCount, lastMessage, label, metadata, muted, null, channelType)

