package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.entity.messages.AutoDeleteMessageEntity
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.entity.messages.ForwardingDetailsDb
import com.sceyt.chatuikit.persistence.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.entity.messages.ParentMessageDb
import com.sceyt.chatuikit.persistence.file_transfer.TransferState

fun SceytMessage.toMessageEntity(unList: Boolean) = MessageEntity(
    tid = getTid(id, tid, incoming),
    // Set id null if message is not sent yet, because id id unique in db
    id = if (id == 0L) null else id,
    channelId = channelId,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt,
    incoming = incoming,
    isTransient = isTransient,
    silent = silent,
    deliveryStatus = deliveryStatus,
    state = state,
    fromId = user?.id,
    markerCount = markerTotals?.toList(),
    mentionedUsersIds = mentionedUsers?.map { it.id },
    parentId = if (parentMessage?.id == 0L) null else parentMessage?.id,
    replyCount = replyCount,
    displayCount = displayCount,
    autoDeleteAt = autoDeleteAt,
    forwardingDetailsDb = forwardingDetails?.toForwardingDetailsDb(),
    unList = unList,
    bodyAttribute = bodyAttributes
)

fun getTid(msgId: Long, tid: Long, incoming: Boolean): Long {
    return if (incoming)
        msgId
    else tid
}

fun SceytMessage.toMessageDb(unList: Boolean): MessageDb {
    val tid = getTid(id, tid, incoming)
    return MessageDb(
        messageEntity = toMessageEntity(unList),
        from = user?.toUserEntity(),
        parent = parentMessage?.toParentMessageEntity(),
        attachments = attachments?.map { it.toAttachmentDb(id, tid, channelId) },
        userMarkers = userMarkers?.map { it.toMarkerEntity() },
        reactions = userReactions?.map { it.toReactionDb() },
        reactionsTotals = reactionTotals?.map { it.toReactionTotalEntity(id) }?.toMutableList(),
        forwardingUser = forwardingDetails?.user?.toUserEntity(),
        pendingReactions = null,
        mentionedUsers = null
    )
}


fun MessageDb.toSceytMessage(): SceytMessage {
    with(messageEntity) {
        return SceytMessage(
            id = id ?: 0,
            tid = tid,
            channelId = channelId,
            body = body,
            type = type,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = updatedAt,
            incoming = incoming,
            isTransient = isTransient,
            silent = silent,
            deliveryStatus = deliveryStatus,
            state = state,
            user = from?.toUser(),
            attachments = attachments?.map { it.toAttachment() },
            userReactions = selfReactions?.map { it.toSceytReaction() },
            reactionTotals = reactionsTotals?.map { it.toReactionTotal() },
            markerTotals = markerCount,
            userMarkers = userMarkers?.map {
                it.toSceytMarker(ClientWrapper.currentUser ?: User(it.userId))
            },
            mentionedUsers = mentionedUsers?.map {
                it.user?.toUser() ?: User(it.link.userId)
            },
            parentMessage = parent?.toSceytMessage(),
            replyCount = replyCount,
            displayCount = displayCount,
            autoDeleteAt = autoDeleteAt,
            forwardingDetails = forwardingDetailsDb?.toForwardingDetails(channelId, forwardingUser?.toUser()),
            pendingReactions = pendingReactions?.map { it.toReactionData() },
            bodyAttributes = bodyAttribute
        )
    }
}

fun ParentMessageDb.toSceytMessage(): SceytMessage {
    return messageEntity.parentMessageToSceytMessage(
        attachments = this@toSceytMessage.attachments?.map { it.toAttachment() }?.toTypedArray(),
        from = this@toSceytMessage.from?.toUser(),
        mentionedUsers = mentionedUsers?.map {
            it.user?.toUser() ?: User(it.link.userId)
        }?.toTypedArray()
    )
}

fun SceytMessage.toParentMessageEntity(): ParentMessageDb {
    return ParentMessageDb(toMessageEntity(true), user?.toUserEntity(), attachments?.map {
        it.toAttachmentDb(id, getTid(id, tid, incoming), channelId)
    }, null)
}

private fun MessageEntity.parentMessageToSceytMessage(
        attachments: Array<SceytAttachment>?,
        from: User?, mentionedUsers: Array<User>?
) = SceytMessage(
    id = id ?: 0,
    tid = tid,
    channelId = channelId,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt,
    incoming = incoming,
    isTransient = isTransient,
    silent = silent,
    deliveryStatus = deliveryStatus,
    state = state,
    user = from,
    attachments = attachments?.toList(),
    userReactions = emptyList(),
    reactionTotals = emptyList(),
    markerTotals = markerCount,
    userMarkers = emptyList(),
    mentionedUsers = mentionedUsers?.toList(),
    parentMessage = null,
    replyCount = replyCount,
    displayCount = displayCount,
    autoDeleteAt = autoDeleteAt,
    forwardingDetails = forwardingDetailsDb?.toForwardingDetails(channelId, null),
    pendingReactions = null,
    bodyAttributes = bodyAttribute
)

fun MessageDb.toMessage(): Message {
    with(messageEntity) {
        return Message(
            id ?: 0,
            tid,
            channelId,
            body,
            type,
            metadata,
            createdAt,
            updatedAt,
            incoming,
            isTransient,
            silent,
            deliveryStatus,
            state,
            from?.toUser(),
            attachments?.map { it.toSdkAttachment(false) }?.toTypedArray(),
            selfReactions?.map { it.toReaction() }?.toTypedArray(),
            reactionsTotals?.map { it.toReactionTotal() }?.toTypedArray(),
            markerCount?.toTypedArray(),
            emptyArray(),
            emptyArray(),
            parent?.toSceytMessage()?.toMessage(),
            replyCount,
            displayCount,
            autoDeleteAt ?: 0L,
            forwardingDetailsDb?.toForwardingDetails(channelId, forwardingUser?.toUser()),
            bodyAttribute?.toTypedArray()
        )
    }
}


fun Message.toSceytUiMessage(isGroup: Boolean? = null): SceytMessage {
    val tid = getTid(id, tid, incoming)
    return SceytMessage(
        id = id,
        tid = tid,
        channelId = channelId,
        body = body,
        type = type,
        metadata = metadata,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        incoming = incoming,
        isTransient = isTransient,
        silent = silent,
        deliveryStatus = deliveryStatus,
        state = state,
        user = user,
        attachments = attachments?.map {
            val transferState: TransferState
            val progress: Float
            if (it.filePath.isNullOrBlank()) {
                transferState = TransferState.PendingDownload
                progress = 0f
            } else {
                transferState = TransferState.Downloaded
                progress = 100f
            }
            it.toSceytAttachment(tid, transferState, progress)
        },
        userReactions = userReactions?.map { it.toSceytReaction() },
        reactionTotals = reactionTotals?.toList(),
        markerTotals = markerTotals?.toList(),
        userMarkers = userMarkers?.map { it.toSceytMarker() },
        mentionedUsers = mentionedUsers?.toList(),
        parentMessage = parentMessage?.toSceytUiMessage(),
        replyCount = replyCount,
        displayCount = displayCount.toShort(),
        autoDeleteAt = autoDeleteAt,
        forwardingDetails = forwardingDetails,
        pendingReactions = null,
        bodyAttributes = bodyAttributes?.toList(),
        isGroup = isGroup ?: false
    )
}

fun SceytMessage.toMessage(): Message {
    return Message(
        id,
        tid,
        channelId,
        body,
        type,
        metadata,
        createdAt,
        updatedAt,
        incoming,
        isTransient,
        silent,
        deliveryStatus,
        state,
        user,
        attachments?.map { it.toAttachment() }?.toTypedArray(),
        userReactions?.map { it.toReaction() }?.toTypedArray(),
        reactionTotals?.toTypedArray(),
        markerTotals?.toTypedArray(),
        userMarkers?.map { it.toMarker() }?.toTypedArray(),
        mentionedUsers?.toTypedArray(),
        parentMessage?.toMessage(),
        replyCount,
        displayCount,
        autoDeleteAt ?: 0L,
        forwardingDetails,
        bodyAttributes?.toTypedArray())
}

fun ForwardingDetails.toForwardingDetailsDb() = ForwardingDetailsDb(
    messageId = messageId,
    userId = user?.id,
    hops = hops
)


fun ForwardingDetailsDb.toForwardingDetails(channelId: Long, user: User?) = ForwardingDetails(
    messageId, channelId,
    user,
    hops
)

fun DraftMessageDb.toDraftMessage() = DraftMessage(
    chatId = draftMessageEntity.chatId,
    message = draftMessageEntity.message,
    createdAt = draftMessageEntity.createdAt,
    mentionUsers = mentionUsers?.map { it.toUser() },
    replyOrEditMessage = replyOrEditMessage?.toSceytMessage(),
    isReply = draftMessageEntity.isReplyMessage ?: false,
    bodyAttributes = draftMessageEntity.styleRanges
)

fun DraftMessageEntity.toDraftMessage(
        mentionUsers: List<User>?,
        replyMessage: SceytMessage?
) = DraftMessage(
    chatId = chatId,
    message = message,
    createdAt = createdAt,
    mentionUsers = mentionUsers,
    replyOrEditMessage = replyMessage,
    isReply = isReplyMessage ?: false,
    bodyAttributes = styleRanges
)

fun SceytMessage.toAutoDeleteMessageEntity() = AutoDeleteMessageEntity(
    messageId = id,
    channelId = channelId,
    autoDeleteAt = autoDeleteAt ?: 0L
)

fun List<SceytMessage>.toAutoDeleteMessageEntities() = map { it.toAutoDeleteMessageEntity() }