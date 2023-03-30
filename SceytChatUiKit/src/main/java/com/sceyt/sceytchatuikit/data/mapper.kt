package com.sceyt.sceytchatuikit.data

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.*
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toUser
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem


fun Channel.toSceytUiChannel(): SceytChannel {
    if (this is GroupChannel) {
        return SceytGroupChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            unreadMentionCount = unreadMentionCount,
            unreadReactionCount = unreadReactionCount,
            lastMessage = lastMessage?.toSceytUiMessage(true),
            label = label,
            metadata = metadata,
            muted = muted(),
            muteExpireDate = muteExpireDate(),
            markedUsUnread = markedAsUnread(),
            channelType = getChannelType(this),
            subject = subject,
            avatarUrl = avatarUrl,
            channelUrl = getChannelUrl(),
            members = lastActiveMembers.map { it.toSceytMember() },
            memberCount = memberCount,
            lastDeliveredMessageId = lastDeliveredMessageId,
            lastReadMessageId = lastReadMessageId,
            messagesDeletionDate = messagesDeletionDate,
            role = myRole(),
            lastMessages = lastMessages?.map { it.toSceytUiMessage() },
            userMessageReactions = getUserMessageReactions()?.toList()
        )
    } else {
        this as DirectChannel
        return SceytDirectChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            unreadMentionCount = unreadMentionCount,
            unreadReactionCount = unreadReactionCount,
            lastMessage = lastMessage?.toSceytUiMessage(false),
            label = label,
            metadata = metadata,
            muted = muted(),
            markedUsUnread = markedAsUnread(),
            peer = peer?.toSceytMember(),
            lastDeliveredMessageId = lastDeliveredMessageId,
            lastReadMessageId = lastReadMessageId,
            channelType = getChannelType(this),
            messagesDeletionDate = messagesDeletionDate,
            lastMessages = lastMessages?.map { it.toSceytUiMessage() },
            userMessageReactions = getUserMessageReactions()?.toList()
        )
    }
}

fun SceytChannel.toGroupChannel(): GroupChannel {
    return when (channelType) {
        ChannelTypeEnum.Private -> {
            this as SceytGroupChannel
            PrivateChannel(
                id, subject, metadata, avatarUrl,
                label, createdAt, updatedAt, members.map { it.toMember() }.toTypedArray(),
                lastMessage?.toMessage(), unreadMessageCount, unreadMentionCount, unreadReactionCount, memberCount, muted, 0,
                markedUsUnread, lastDeliveredMessageId, lastReadMessageId, messagesDeletionDate, null,
                lastMessages?.map { it.toMessage() }?.toTypedArray(), userMessageReactions?.toTypedArray())
        }
        ChannelTypeEnum.Public -> {
            this as SceytGroupChannel
            PublicChannel(id, channelUrl, subject, metadata, avatarUrl,
                label, createdAt, updatedAt, members.map { it.toMember() }.toTypedArray(),
                lastMessage?.toMessage(), unreadMessageCount, unreadMentionCount, unreadReactionCount, memberCount, muted, 0,
                markedUsUnread, lastDeliveredMessageId, lastReadMessageId, messagesDeletionDate, null,
                lastMessages?.map { it.toMessage() }?.toTypedArray(), userMessageReactions?.toTypedArray())
        }
        else -> throw RuntimeException("Channel is direct channel")
    }
}

fun Member.toSceytMember() = SceytMember(
    role = role,
    user = this
)

fun SceytMember.toMember(): Member {
    return Member(role, user)
}

fun GroupChannel.getChannelUrl(): String {
    return if (this is PublicChannel)
        uri
    else ""
}

fun Attachment.toSceytAttachment(messageTid: Long, transferState: TransferState, progress: Float = 0f) = SceytAttachment(
    id = id,
    tid = tid,
    messageTid = messageTid,
    messageId = messageId,
    userId = userId,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = uploadedFileSize,
    createdAt = createdAt,
    url = url,
    filePath = filePath,
    transferState = transferState,
    progressPercent = progress,
)


fun SceytAttachment.toAttachment(): Attachment = Attachment(
    id ?: 0,
    messageId,
    name,
    type,
    metadata,
    fileSize,
    url,
    createdAt,
    userId,
    tid,
    filePath,
    false,
)


fun SceytAttachment.toFileListItem(message: SceytMessage): FileListItem {
    return when (type) {
        AttachmentTypeEnum.Image.value() -> FileListItem.Image(this, message)
        AttachmentTypeEnum.Video.value() -> FileListItem.Video(this, message)
        else -> FileListItem.File(this, message)
    }
}

fun Presence.hasDiff(other: Presence): Boolean {
    return state != other.state || status != other.status || lastActiveAt != other.lastActiveAt
}

fun DraftMessageDb.toDraftMessage() = DraftMessage(
    chatId = draftMessageEntity.chatId,
    message = draftMessageEntity.message,
    createdAt = draftMessageEntity.createdAt,
    metadata = draftMessageEntity.metadata,
    mentionUsers = users?.map { it.toUser() }
)

fun DraftMessageEntity.toDraftMessage(mentionUsers: List<User>?) = DraftMessage(
    chatId = chatId,
    message = message,
    createdAt = createdAt,
    metadata = metadata,
    mentionUsers = mentionUsers
)

fun User.copy() = User(
    id, firstName, lastName, avatarURL, metadata, presence.copy(), activityState, blocked
)

fun Presence.copy() = Presence(
    state, status, lastActiveAt
)