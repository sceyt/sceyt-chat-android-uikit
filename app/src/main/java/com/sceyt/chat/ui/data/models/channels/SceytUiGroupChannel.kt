package com.sceyt.chat.ui.data.models.channels

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import java.util.*

class SceytUiGroupChannel(
        id: Long = 0,
        createdAt: Long = 0,
        updatedAt: Long = 0,
        unreadMessageCount: Long = 0,
        lastMessage: Message? = null,
        label: String? = null,
        metadata: String? = null,
        muted: Boolean = false,
        muteExpireDate: Date? = null,
        channelType: ChannelTypeEnum,
        var subject: String? = "",
        var avatarUrl: String? = "",
        var members: List<Member?>?,
        var memberCount: Long = 0L,
) : SceytUiChannel(id, createdAt, updatedAt, unreadMessageCount, lastMessage, label, metadata, muted, muteExpireDate, channelType)

