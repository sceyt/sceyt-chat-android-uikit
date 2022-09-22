package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getPresentableName
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
        var peer: SceytMember? = null,
) : SceytChannel(id, createdAt, updatedAt, unreadMessageCount, lastMessage, label, metadata, muted, null, channelType) {

    override val channelSubject: String
        get() = peer?.getPresentableName() ?: ""

    override val iconUrl: String?
        get() = peer?.avatarUrl

    override val isGroup: Boolean
        get() = false

    override fun clone(): SceytChannel {
        return SceytDirectChannel(id, metadata, label, createdAt, updatedAt, unreadMessageCount, lastMessage, muted, channelType, peer)
    }
}

