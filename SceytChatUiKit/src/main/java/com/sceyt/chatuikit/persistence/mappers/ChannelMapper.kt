package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.toSceytMember
import com.sceyt.chatuikit.persistence.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.extensions.isSelf

fun SceytChannel.toChannelEntity() = ChannelEntity(
    id = id,
    parentChannelId = parentChannelId,
    uri = uri,
    type = type,
    subject = subject,
    avatarUrl = avatarUrl,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messagesClearedAt = messagesClearedAt,
    memberCount = memberCount,
    createdById = createdBy?.id,
    userRole = userRole,
    unread = unread,
    newMessageCount = newMessageCount,
    newMentionCount = newMentionCount,
    newReactedMessageCount = newReactedMessageCount,
    hidden = hidden,
    archived = archived,
    muted = muted,
    mutedTill = mutedTill,
    pinnedAt = pinnedAt,
    lastReceivedMessageId = lastReceivedMessageId,
    lastDisplayedMessageId = lastDisplayedMessageId,
    messageRetentionPeriod = messageRetentionPeriod,
    lastMessageTid = getTid(lastMessage?.id, lastMessage?.tid, lastMessage?.incoming),
    lastMessageAt = lastMessage?.createdAt,
    pending = pending,
    isSelf = isSelf()
)

private fun getTid(msgId: Long?, tid: Long?, incoming: Boolean?): Long? {
    return if (incoming == true)
        msgId
    else tid
}

fun ChannelDb.toChannel(): SceytChannel {
    with(channelEntity) {
        return SceytChannel(
            id = id,
            parentChannelId = parentChannelId,
            uri = uri,
            type = type,
            subject = subject,
            avatarUrl = avatarUrl,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
            messagesClearedAt = messagesClearedAt,
            memberCount = memberCount,
            createdBy = createdBy?.toSceytUser(),
            userRole = userRole,
            unread = unread,
            newMessageCount = newMessageCount,
            newMentionCount = newMentionCount,
            newReactedMessageCount = newReactedMessageCount,
            hidden = hidden,
            archived = archived,
            muted = muted,
            mutedTill = mutedTill,
            pinnedAt = pinnedAt,
            lastReceivedMessageId = lastReceivedMessageId,
            lastDisplayedMessageId = lastDisplayedMessageId,
            messageRetentionPeriod = messageRetentionPeriod,
            lastMessage = lastMessage?.toSceytMessage(),
            messages = emptyList(),
            members = members?.map { it.toSceytMember() },
            newReactions = newReactions?.map { it.toSceytReaction() },
            pendingReactions = pendingReactions?.map { it.toReactionData() },
            pending = pending,
            draftMessage = draftMessage?.toDraftMessage()
        )
    }
}

fun Channel.toSceytUiChannel(): SceytChannel {
    return SceytChannel(
        id = id,
        parentChannelId = parentChannelId,
        uri = uri,
        type = type,
        subject = subject,
        avatarUrl = avatarUrl,
        metadata = metadata,
        createdAt = createdAt,
        updatedAt = updatedAt,
        messagesClearedAt = messagesClearedAt,
        memberCount = memberCount,
        createdBy = createdBy?.toSceytUser(),
        userRole = userRole,
        unread = isUnread,
        newMessageCount = newMessageCount,
        newMentionCount = newMentionCount,
        newReactedMessageCount = newReactedMessageCount,
        hidden = isHidden,
        archived = isArchived,
        muted = isMuted,
        mutedTill = mutedTill,
        pinnedAt = pinnedAt,
        lastReceivedMessageId = lastReceivedMessageId,
        lastDisplayedMessageId = lastDisplayedMessageId,
        messageRetentionPeriod = messageRetentionPeriod,
        lastMessage = lastMessage?.toSceytUiMessage(),
        messages = messages?.map { it.toSceytUiMessage() },
        members = members?.map { it.toSceytMember() },
        newReactions = newReactions.map { it.toSceytReaction() },
        pendingReactions = null,
        pending = false,
        draftMessage = null
    )
}

fun createPendingDirectChannelData(
        channelId: Long,
        createdBy: SceytUser,
        members: List<SceytMember>,
        role: String,
        metadata: String
) = SceytChannel(
    id = channelId,
    parentChannelId = null,
    uri = null,
    type = ChannelTypeEnum.Direct.value,
    subject = null,
    avatarUrl = null,
    metadata = metadata,
    createdAt = System.currentTimeMillis(),
    updatedAt = 0,
    messagesClearedAt = 0,
    memberCount = members.size.toLong(),
    createdBy = createdBy,
    userRole = role,
    unread = false,
    newMessageCount = 0,
    newMentionCount = 0,
    newReactedMessageCount = 0,
    hidden = false,
    archived = false,
    muted = false,
    mutedTill = 0,
    pinnedAt = null,
    lastReceivedMessageId = 0,
    lastDisplayedMessageId = 0,
    messageRetentionPeriod = 0,
    lastMessage = null,
    messages = null,
    members = members,
    newReactions = null,
    pendingReactions = null,
    pending = true,
    draftMessage = null
)