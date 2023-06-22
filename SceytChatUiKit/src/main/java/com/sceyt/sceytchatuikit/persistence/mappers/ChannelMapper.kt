package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.channel.Channel
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.toDraftMessage
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity

fun SceytChannel.toChannelEntity() = ChannelEntity(
    id = id,
    parentId = parentId,
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
    role = role,
    unread = unread,
    newMessageCount = newMessageCount,
    newMentionCount = newMentionCount,
    newReactionCount = newReactionCount,
    hidden = hidden,
    archived = archived,
    muted = muted,
    mutedUntil = mutedUntil,
    pinnedAt = pinnedAt,
    lastReceivedMessageId = lastReceivedMessageId,
    lastDisplayedMessageId = lastDisplayedMessageId,
    messageRetentionPeriod = messageRetentionPeriod,
    lastMessageTid = getTid(lastMessage?.id, lastMessage?.tid, lastMessage?.incoming),
    lastMessageAt = lastMessage?.createdAt,
    pending = pending
)

private fun getTid(msgId: Long?, tid: Long?, incoming: Boolean?): Long? {
    return if (incoming == true)
        msgId
    else tid
}

fun Channel.toChannelEntity() = ChannelEntity(
    id = id,
    parentId = parentId,
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
    role = role,
    unread = isUnread,
    newMessageCount = newMessageCount,
    newMentionCount = newMentionCount,
    newReactionCount = newReactionCount,
    hidden = isHidden,
    archived = isArchived,
    muted = isMuted,
    mutedUntil = muteUntil,
    pinnedAt = pinnedAt,
    lastReceivedMessageId = lastReceivedMessageId,
    lastDisplayedMessageId = lastDisplayedMessageId,
    messageRetentionPeriod = messageRetentionPeriod,
    lastMessageTid = getTid(lastMessage?.id, lastMessage?.tid, lastMessage?.incoming),
    lastMessageAt = lastMessage?.createdAt?.time,
    pending = false
)

fun ChannelDb.toChannel(): SceytChannel {
    with(channelEntity) {
        return SceytChannel(
            id = id,
            parentId = parentId,
            uri = uri,
            type = type,
            subject = subject,
            avatarUrl = avatarUrl,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
            messagesClearedAt = messagesClearedAt,
            memberCount = memberCount,
            createdBy = createdBy?.toUser(),
            role = role,
            unread = unread,
            newMessageCount = newMessageCount,
            newMentionCount = newMentionCount,
            newReactionCount = newReactionCount,
            hidden = hidden,
            archived = archived,
            muted = muted,
            mutedUntil = mutedUntil,
            pinnedAt = pinnedAt,
            lastReceivedMessageId = lastReceivedMessageId,
            lastDisplayedMessageId = lastDisplayedMessageId,
            messageRetentionPeriod = messageRetentionPeriod,
            lastMessage = lastMessage?.toSceytMessage(),
            messages = emptyList(),
            members = members?.map { it.toSceytMember() },
            newReactions = newReactions?.map { it.toReaction() },
            pending = pending
        ).apply {
            draftMessage = this@toChannel.draftMessage?.toDraftMessage()
        }
    }
}

fun Channel.toSceytUiChannel(): SceytChannel {
    return SceytChannel(
        id = id,
        parentId = parentId,
        uri = uri,
        type = type,
        subject = subject,
        avatarUrl = avatarUrl,
        metadata = metadata,
        createdAt = createdAt,
        updatedAt = updatedAt,
        messagesClearedAt = messagesClearedAt,
        memberCount = memberCount,
        createdBy = createdBy,
        role = role,
        unread = isUnread,
        newMessageCount = newMessageCount,
        newMentionCount = newMentionCount,
        newReactionCount = newReactionCount,
        hidden = isHidden,
        archived = isArchived,
        muted = isMuted,
        mutedUntil = muteUntil,
        pinnedAt = pinnedAt,
        lastReceivedMessageId = lastReceivedMessageId,
        lastDisplayedMessageId = lastDisplayedMessageId,
        messageRetentionPeriod = messageRetentionPeriod,
        lastMessage = lastMessage?.toSceytUiMessage(),
        messages = messages?.map { it.toSceytUiMessage() },
        members = members?.map { it.toSceytMember() },
        newReactions = newReactions,
        pending = false
    )
}