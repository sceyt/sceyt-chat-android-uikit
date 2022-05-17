package com.sceyt.chat.ui.data

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.models.channels.SceytUiChannel
import com.sceyt.chat.ui.data.models.channels.SceytUiDirectChannel
import com.sceyt.chat.ui.data.models.channels.SceytUiGroupChannel
import com.sceyt.chat.ui.data.models.channels.getChannelType
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage


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

fun Message.toSceytUiMessage(isGroup: Boolean = false) = SceytUiMessage(
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
    this.isGroup = isGroup
}