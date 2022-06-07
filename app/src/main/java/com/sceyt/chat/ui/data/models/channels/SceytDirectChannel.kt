package com.sceyt.chat.ui.data.models.channels

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import kotlinx.parcelize.Parcelize

@Parcelize
class SceytDirectChannel(
        override var id: Long = 0,
        override var metadata: String? = null,
        override var label: String? = null,
        override var createdAt: Long,
        override var updatedAt: Long,
        override var unreadMessageCount: Long,
        override var lastMessage: SceytMessage? = null,
        override var muted: Boolean = false,
        override var channelType: ChannelTypeEnum = ChannelTypeEnum.Direct,
        var peer: Member? = null,
) : SceytChannel(id, createdAt, updatedAt, unreadMessageCount, lastMessage, label, metadata, muted, null, channelType)

