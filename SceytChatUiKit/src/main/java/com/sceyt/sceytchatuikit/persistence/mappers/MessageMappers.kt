package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toAttachment
import com.sceyt.sceytchatuikit.data.toSceytAttachment
import com.sceyt.sceytchatuikit.persistence.entity.messages.ForwardingDetailsDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ParentMessageDb
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import java.util.Date

fun SceytMessage.toMessageEntity(unList: Boolean) = MessageEntity(
    tid = getTid(id, tid, incoming),
    // Set id null if message is not sent yet, because id id unique in db
    id = if (id == 0L) null else id,
    channelId = channelId,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt.time,
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
    unList = unList
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
            updatedAt = Date(updatedAt),
            incoming = incoming,
            isTransient = isTransient,
            silent = silent,
            deliveryStatus = deliveryStatus,
            state = state,
            user = from?.toUser(),
            attachments = attachments?.map { it.toAttachment() }?.toTypedArray(),
            userReactions = selfReactions?.map { it.toSceytReaction() }?.toTypedArray(),
            reactionTotals = reactionsTotals?.map { it.toReactionTotal() }?.toTypedArray(),
            markerTotals = markerCount?.toTypedArray(),
            userMarkers = userMarkers?.map {
                it.toMarker(ClientWrapper.currentUser ?: User(it.userId))
            }?.toTypedArray(),
            mentionedUsers = mentionedUsers?.map {
                it.user?.toUser() ?: User(it.link.userId)
            }?.toTypedArray(),
            parentMessage = parent?.toSceytMessage(),
            replyCount = replyCount,
            displayCount = displayCount,
            autoDeleteAt = autoDeleteAt,
            forwardingDetails = forwardingDetailsDb?.toForwardingDetails(channelId, forwardingUser?.toUser()),
            pendingReactions = pendingReactions?.map { it.toReactionData() }
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

fun Marker.toMarkerEntity(): MarkerEntity {
    return MarkerEntity(messageId, user.id, name, createdAt)
}

private fun MessageEntity.parentMessageToSceytMessage(attachments: Array<SceytAttachment>?,
                                                      from: User?, mentionedUsers: Array<User>?) = SceytMessage(
    id = id ?: 0,
    tid = tid,
    channelId = channelId,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = Date(updatedAt),
    incoming = incoming,
    isTransient = isTransient,
    silent = silent,
    deliveryStatus = deliveryStatus,
    state = state,
    user = from,
    attachments = attachments,
    userReactions = emptyArray(),
    reactionTotals = emptyArray(),
    markerTotals = markerCount?.toTypedArray(),
    userMarkers = emptyArray(),
    mentionedUsers = mentionedUsers,
    parentMessage = null,
    replyCount = replyCount,
    displayCount = displayCount,
    autoDeleteAt = autoDeleteAt,
    forwardingDetails = forwardingDetailsDb?.toForwardingDetails(channelId, null),
    pendingReactions = null
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
            messageEntity.displayCount,
            messageEntity.autoDeleteAt ?: 0L,
            messageEntity.forwardingDetailsDb?.toForwardingDetails(channelId, forwardingUser?.toUser())
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
        updatedAt = updatedAt,
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
        }?.toTypedArray(),
        userReactions = userReactions?.map { it.toSceytReaction() }?.toTypedArray(),
        reactionTotals = reactionTotals,
        markerTotals = markerTotals,
        userMarkers = userMarkers,
        mentionedUsers = mentionedUsers,
        parentMessage = parentMessage?.toSceytUiMessage(),
        replyCount = replyCount,
        displayCount = displayCount.toShort(),
        autoDeleteAt = autoDeleteAt,
        forwardingDetails = forwardingDetails,
        pendingReactions = null
    ).apply {
        isGroup?.let {
            this.isGroup = it
        }
    }
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
        updatedAt.time,
        incoming,
        isTransient,
        silent,
        deliveryStatus,
        state,
        user,
        attachments?.map { it.toAttachment() }?.toTypedArray(),
        userReactions?.map { it.toReaction() }?.toTypedArray(),
        reactionTotals,
        markerTotals,
        userMarkers,
        mentionedUsers,
        parentMessage?.toMessage(),
        replyCount,
        displayCount,
        autoDeleteAt ?: 0L,
        forwardingDetails)
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
