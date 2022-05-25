package com.sceyt.chat.ui.data

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.data.models.channels.SceytUiChannel
import com.sceyt.chat.ui.data.models.channels.SceytUiDirectChannel
import com.sceyt.chat.ui.data.models.channels.SceytUiGroupChannel
import com.sceyt.chat.ui.data.models.channels.getChannelType
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory


fun Channel.toSceytUiChannel(): SceytUiChannel {
    if (this is GroupChannel)
        return SceytUiGroupChannel(
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
        return SceytUiDirectChannel(
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

fun Message.toSceytUiMessage(isGroup: Boolean? = null) = SceytUiMessage(
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

fun SceytUiMessage.toMessage() = Message(
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

fun Attachment.toFileListItem(message: SceytUiMessage): FileListItem {
    return when (type) {
        "image" -> FileListItem.Image(this, message)
        "video" -> FileListItem.Video(this, message)
        else -> FileListItem.File(this, message)
    }
}