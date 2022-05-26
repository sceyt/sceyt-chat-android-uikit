package com.sceyt.chat.ui.data

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.models.channels.SceytGroupChannel
import com.sceyt.chat.ui.data.models.channels.getChannelType
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem


fun Channel.toSceytUiChannel(): SceytChannel {
    if (this is GroupChannel)
        return SceytGroupChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            lastMessage = lastMessage,
            label = label,
            metadata = metadata,
            muted = muted(),
            muteExpireDate = muteExpireDate(),
            channelType = getChannelType(this),
            subject = subject,
            avatarUrl = avatarUrl,
            members = members,
            memberCount = memberCount
        )
    else {
        this as DirectChannel
        return SceytDirectChannel(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadMessageCount = unreadMessageCount,
            lastMessage = lastMessage,
            label = label,
            metadata = metadata,
            muted = muted(),
            peer = peer,
            channelType = getChannelType(this),
        )
    }
}

fun Message.toSceytUiMessage(isGroup: Boolean? = null) = SceytMessage(
    id = id,
    tid = tid,
    channelId = channelId,
    to = to,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt.time,
    updatedAt = updatedAt,
    incoming = incoming,
    receipt = receipt,
    isTransient = isTransient,
    silent = silent,
    deliveryStatus = deliveryStatus,
    state = state,
    from = from,
    attachments = attachments,
    lastReactions = lastReactions,
    selfReactions = selfReactions,
    reactionScores = reactionScores,
    markerCount = markerCount,
    selfMarkers = selfMarkers,
    mentionedUsers = mentionedUsers,
    parent = parent,
    replyInThread = replyInThread,
    replyCount = replyCount
).apply {
    isGroup?.let {
        this.isGroup = it
    }
}

fun SceytMessage.toMessage() = Message(
    id,
    tid,
    channelId,
    to,
    body,
    type,
    metadata,
    createdAt,
    updatedAt.time,
    incoming,
    receipt,
    isTransient,
    silent,
    deliveryStatus,
    state,
    from,
    attachments,
    lastReactions,
    selfReactions,
    reactionScores,
    markerCount,
    selfMarkers,
    mentionedUsers,
    parent,
    replyInThread,
    replyCount
)

fun Attachment.toFileListItem(message: SceytMessage): FileListItem {
    return when (type) {
        "image" -> FileListItem.Image(this, message)
        "video" -> FileListItem.Video(this, message)
        else -> FileListItem.File(this, message)
    }
}