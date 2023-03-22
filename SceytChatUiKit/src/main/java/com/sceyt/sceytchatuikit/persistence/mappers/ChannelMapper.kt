package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.role.Role
import com.sceyt.sceytchatuikit.data.getChannelUrl
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.toDraftMessage
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import java.util.*

fun SceytChannel.toChannelEntity(): ChannelEntity {
    var memberCount = 1L
    val myRole: String?
    var channelUrl: String? = null
    if (isGroup) {
        memberCount = (this as SceytGroupChannel).memberCount
        myRole = role?.name
        channelUrl = this.channelUrl
    } else myRole = RoleTypeEnum.Owner.toString()

    return ChannelEntity(
        id = id,
        type = channelType,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadMessageCount = unreadMessageCount,
        unreadMentionCount = unreadMentionCount,
        unreadReactionCount = unreadReactionCount,
        lastMessageTid = getTid(lastMessage?.id, lastMessage?.tid, lastMessage?.incoming),
        lastMessageAt = lastMessage?.createdAt,
        label = label,
        metadata = metadata,
        muted = muted,
        muteExpireDate = muteExpireDate?.time,
        markedUsUnread = markedUsUnread,
        subject = if (isGroup) channelSubject else null,
        avatarUrl = getChannelAvatarUrl(),
        memberCount = memberCount,
        role = myRole,
        lastDeliveredMessageId = lastDeliveredMessageId,
        lastReadMessageId = lastReadMessageId,
        channelUrl = channelUrl,
        messagesDeletionDate = messagesDeletionDate
    )
}

private fun getTid(msgId: Long?, tid: Long?, incoming: Boolean?): Long? {
    return if (incoming == true)
        msgId
    else tid
}

fun Channel.toChannelEntity(): ChannelEntity {
    var memberCount = 1L
    var subject = ""
    val avatarUrl: String
    var channelUrl: String? = null
    val myRole: String?

    if (this is GroupChannel) {
        memberCount = this.memberCount
        subject = this.subject
        avatarUrl = this.avatarUrl
        channelUrl = this.getChannelUrl()
        myRole = myRole()?.name
    } else {
        this as DirectChannel
        avatarUrl = this.peer.avatarURL
        myRole = RoleTypeEnum.Owner.toString()
    }

    return ChannelEntity(
        id = id,
        type = getChannelType(this),
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadMessageCount = unreadMessageCount,
        unreadMentionCount = unreadMentionCount,
        unreadReactionCount = unreadReactionCount,
        lastMessageTid = getTid(lastMessage?.id, lastMessage?.tid, lastMessage?.incoming),
        lastMessageAt = lastMessage?.createdAt?.time,
        label = label,
        metadata = metadata,
        muted = muted(),
        muteExpireDate = muteExpireDate()?.time,
        markedUsUnread = markedAsUnread(),
        subject = subject,
        avatarUrl = avatarUrl,
        memberCount = memberCount,
        role = myRole,
        channelUrl = channelUrl,
        lastDeliveredMessageId = lastDeliveredMessageId,
        lastReadMessageId = lastReadMessageId,
        messagesDeletionDate = messagesDeletionDate
    )
}

fun ChannelDb.toChannel(): SceytChannel {
    with(channelEntity) {
        return when (type) {
            ChannelTypeEnum.Private, ChannelTypeEnum.Public ->
                SceytGroupChannel(
                    id = id,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    unreadMessageCount = unreadMessageCount,
                    unreadMentionCount = unreadMentionCount,
                    unreadReactionCount = unreadReactionCount,
                    lastMessage = lastMessage?.toSceytMessage(),
                    label = label,
                    metadata = metadata,
                    muted = muted,
                    muteExpireDate = Date(muteExpireDate ?: 0),
                    markedUsUnread = markedUsUnread,
                    channelType = type,
                    subject = subject,
                    avatarUrl = avatarUrl,
                    channelUrl = channelUrl,
                    members = members?.map { it.toSceytMember() } ?: arrayListOf(),
                    memberCount = memberCount,
                    lastDeliveredMessageId = lastDeliveredMessageId,
                    lastReadMessageId = lastReadMessageId,
                    messagesDeletionDate = messagesDeletionDate,
                    lastMessages = emptyList(),
                    role = role?.let { Role(it) },
                    userMessageReactions = usersReactions?.map { it.toReaction() },
                )
            ChannelTypeEnum.Direct -> SceytDirectChannel(
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt,
                unreadMessageCount = unreadMessageCount,
                unreadMentionCount = unreadMentionCount,
                unreadReactionCount = unreadReactionCount,
                lastMessage = lastMessage?.toSceytMessage(),
                label = label,
                metadata = metadata,
                muted = muted,
                peer = members?.firstOrNull()?.toSceytMember(),
                markedUsUnread = markedUsUnread,
                lastDeliveredMessageId = lastDeliveredMessageId,
                lastReadMessageId = lastReadMessageId,
                messagesDeletionDate = messagesDeletionDate,
                lastMessages = emptyList(),
                userMessageReactions = usersReactions?.map { it.toReaction() },
            )
        }.apply {
            draftMessage = this@toChannel.draftMessage?.toDraftMessage()
        }
    }
}