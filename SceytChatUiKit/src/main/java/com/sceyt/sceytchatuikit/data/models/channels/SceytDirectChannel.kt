package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import kotlinx.parcelize.Parcelize

@Parcelize
class SceytDirectChannel(
        override var id: Long,
        override var metadata: String?,
        override var label: String?,
        override var createdAt: Long,
        override var updatedAt: Long,
        override var unreadMessageCount: Long,
        override var lastMessage: SceytMessage?,
        override var muted: Boolean,
        override var markedUsUnread: Boolean,
        override var channelType: ChannelTypeEnum = ChannelTypeEnum.Direct,
        var peer: SceytMember?,
) : SceytChannel(id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    unreadMessageCount = unreadMessageCount,
    lastMessage = lastMessage,
    label = label,
    metadata = metadata,
    muted = muted,
    muteExpireDate = null,
    markedUsUnread = markedUsUnread,
    channelType = channelType) {

    override val channelSubject: String
        get() = peer?.getPresentableName() ?: ""

    override val iconUrl: String?
        get() = peer?.avatarUrl

    override val isGroup: Boolean
        get() = false

    override fun clone(): SceytChannel {
        return SceytDirectChannel(id = id,
            metadata = metadata,
            label = label,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            lastMessage = lastMessage?.clone(),
            markedUsUnread = markedUsUnread,
            muted = muted,
            channelType = channelType,
            peer = peer)
    }
}

