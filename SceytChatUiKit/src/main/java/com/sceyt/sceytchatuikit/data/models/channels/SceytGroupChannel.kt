package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
class SceytGroupChannel(
        override var id: Long,
        override var createdAt: Long,
        override var updatedAt: Long,
        override var unreadMessageCount: Long,
        override var lastMessage: SceytMessage?,
        override var label: String?,
        override var metadata: String?,
        override var muted: Boolean,
        override var muteExpireDate: Date?,
        override var markedUsUnread: Boolean,
        override var lastDeliveredMessageId: Long,
        override var lastReadMessageId: Long,
        override var channelType: ChannelTypeEnum,
        var subject: String?,
        var avatarUrl: String?,
        var channelUrl: String?,
        var members: List<SceytMember>,
        var memberCount: Long
) : SceytChannel(id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    unreadMessageCount = unreadMessageCount,
    lastMessage = lastMessage,
    label = label,
    metadata = metadata,
    muted = muted,
    muteExpireDate = muteExpireDate,
    markedUsUnread = markedUsUnread,
    lastDeliveredMessageId = lastDeliveredMessageId,
    lastReadMessageId = lastReadMessageId,
    channelType = channelType) {

    override val channelSubject: String
        get() = subject ?: ""

    override val iconUrl: String?
        get() = avatarUrl

    override val isGroup: Boolean
        get() = true

    override fun clone(): SceytChannel {
        return SceytGroupChannel(id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            lastMessage = lastMessage?.clone(),
            label = label,
            metadata = metadata,
            muted = muted,
            muteExpireDate = muteExpireDate,
            markedUsUnread = markedUsUnread,
            channelType = channelType,
            subject = subject,
            avatarUrl = avatarUrl,
            channelUrl = channelUrl,
            members = members.map { it.copy() },
            memberCount = memberCount,
            lastDeliveredMessageId = lastDeliveredMessageId,
            lastReadMessageId = lastReadMessageId)
    }
}

