package com.sceyt.sceytchatuikit.data

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.*
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.Presence
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem


fun Channel.toSceytUiChannel(): SceytChannel {
    if (this is GroupChannel)
        return SceytGroupChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
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
            members = members.map { it.toSceytMember() },
            memberCount = memberCount,
            lastDeliveredMessageId = lastDeliveredMessageId,
            lastReadMessageId = lastReadMessageId
        )
    else {
        this as DirectChannel
        return SceytDirectChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            lastMessage = lastMessage?.toSceytUiMessage(false),
            label = label,
            metadata = metadata,
            muted = muted(),
            markedUsUnread = markedAsUnread(),
            peer = peer?.toSceytMember(),
            lastDeliveredMessageId = lastDeliveredMessageId,
            lastReadMessageId = lastReadMessageId,
            channelType = getChannelType(this),
        )
    }
}

fun SceytChannel.toGroupChannel(): GroupChannel {
    return when (channelType) {
        ChannelTypeEnum.Private -> {
            this as SceytGroupChannel
            PrivateChannel(id, subject, metadata, avatarUrl,
                label, createdAt, updatedAt, members.map { it.toMember() }.toTypedArray(),
                lastMessage?.toMessage(), unreadMessageCount, memberCount, muted, 0,
                markedUsUnread, lastDeliveredMessageId, lastReadMessageId)
        }
        ChannelTypeEnum.Public -> {
            this as SceytGroupChannel
            PublicChannel(id, channelUrl, subject, metadata, avatarUrl,
                label, createdAt, updatedAt, members.map { it.toMember() }.toTypedArray(),
                lastMessage?.toMessage(), unreadMessageCount, memberCount, muted, 0,
                markedUsUnread, lastDeliveredMessageId, lastReadMessageId)
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

fun Attachment.toSceytAttachment() = SceytAttachment(
    tid = tid,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = uploadedFileSize,
    url = url
)


fun SceytAttachment.toSceytAttachment() = Attachment.Builder(url, type)
    .setMetadata(metadata)
    .setName(name)
    .withTid(tid)
    .build()


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
