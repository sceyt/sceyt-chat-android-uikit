package com.sceyt.chat.ui.data.models.channels

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
class SceytGroupChannel(
        override var id: Long = 0,
        override var createdAt: Long = 0,
        override var updatedAt: Long = 0,
        override var unreadMessageCount: Long = 0,
        override var lastMessage: Message? = null,
        override var label: String? = null,
        override var metadata: String? = null,
        override var muted: Boolean = false,
        override var muteExpireDate: Date? = null,
        override var channelType: ChannelTypeEnum,
        var subject: String? = "",
        var avatarUrl: String? = "",
        var members: List<Member>,
        var memberCount: Long = 0L,
) : SceytChannel(id, createdAt, updatedAt, unreadMessageCount, lastMessage, label, metadata, muted, muteExpireDate, channelType)

