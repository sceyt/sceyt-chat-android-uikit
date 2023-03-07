package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.role.Role
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
class SceytGroupChannel(
        override var id: Long,
        override var createdAt: Long,
        override var updatedAt: Long,
        override var unreadMessageCount: Long,
        override var unreadMentionCount: Long,
        override var unreadReactionCount: Long,
        override var lastMessage: SceytMessage?,
        override var label: String?,
        override var metadata: String?,
        override var muted: Boolean,
        override var muteExpireDate: Date?,
        override var markedUsUnread: Boolean,
        override var lastDeliveredMessageId: Long,
        override var lastReadMessageId: Long,
        override var channelType: ChannelTypeEnum,
        override var messagesDeletionDate: Long,
        override var lastMessages: List<SceytMessage>?,
        var role: Role?,
        var subject: String?,
        var avatarUrl: String?,
        var channelUrl: String?,
        var members: List<SceytMember>,
        var memberCount: Long
) : SceytChannel(id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    unreadMessageCount = unreadMessageCount,
    unreadMentionCount = unreadMentionCount,
    unreadReactionCount = unreadReactionCount,
    lastMessage = lastMessage,
    label = label,
    metadata = metadata,
    muted = muted,
    muteExpireDate = muteExpireDate,
    markedUsUnread = markedUsUnread,
    lastDeliveredMessageId = lastDeliveredMessageId,
    lastReadMessageId = lastReadMessageId,
    channelType = channelType,
    messagesDeletionDate = messagesDeletionDate,
    lastMessages = lastMessages) {

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
            unreadMentionCount = unreadMentionCount,
            unreadReactionCount = unreadReactionCount,
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
            lastReadMessageId = lastReadMessageId,
            messagesDeletionDate = messagesDeletionDate,
            lastMessages = lastMessages,
            role = role)
    }
}

