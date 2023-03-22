package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.message.Reaction
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
        override var unreadMentionCount: Long,
        override var unreadReactionCount: Long,
        override var lastMessage: SceytMessage?,
        override var muted: Boolean,
        override var markedUsUnread: Boolean,
        override var lastDeliveredMessageId: Long,
        override var lastReadMessageId: Long,
        override var channelType: ChannelTypeEnum = ChannelTypeEnum.Direct,
        override var messagesDeletionDate: Long,
        override var lastMessages: List<SceytMessage>?,
        override var userMessageReactions: List<Reaction>?,
        var peer: SceytMember?,
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
    muteExpireDate = null,
    markedUsUnread = markedUsUnread,
    lastDeliveredMessageId = lastDeliveredMessageId,
    lastReadMessageId = lastReadMessageId,
    channelType = channelType,
    messagesDeletionDate = messagesDeletionDate,
    lastMessages = lastMessages,
    userMessageReactions = userMessageReactions) {

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
            unreadMentionCount = unreadMentionCount,
            unreadReactionCount = unreadReactionCount,
            lastMessage = lastMessage?.clone(),
            markedUsUnread = markedUsUnread,
            muted = muted,
            channelType = channelType,
            peer = peer?.copy(),
            lastDeliveredMessageId = lastDeliveredMessageId,
            lastReadMessageId = lastReadMessageId,
            messagesDeletionDate = messagesDeletionDate,
            lastMessages = lastMessages,
            userMessageReactions = userMessageReactions).also {
            it.draftMessage = draftMessage
        }
    }
}

