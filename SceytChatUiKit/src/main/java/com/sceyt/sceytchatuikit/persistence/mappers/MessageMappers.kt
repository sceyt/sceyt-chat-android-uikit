package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toAttachment
import com.sceyt.sceytchatuikit.data.toSceytAttachment
import com.sceyt.sceytchatuikit.persistence.entity.messages.ForwardingDetailsDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ParentMessageDb
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import java.util.*

fun SceytMessage.toMessageEntity(isParentMessage: Boolean) = MessageEntity(
    tid = getTid(id, tid, incoming),
    id = id,
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
    userMarkers = userMarkers?.toList(),
    parentId = if (parentMessage?.id == 0L) null else parentMessage?.id,
    replyCount = replyCount,
    displayCount = displayCount,
    autoDeleteDate = autoDeleteDate,
    forwardingDetailsDb = forwardingDetails?.toForwardingDetailsDb(),
    isParentMessage = isParentMessage
)

fun getTid(msgId: Long, tid: Long, incoming: Boolean): Long {
    return if (incoming)
        msgId
    else tid
}


fun SceytMessage.toMessageDb(isParentMessage: Boolean): MessageDb {
    val tid = getTid(id, tid, incoming)
    return MessageDb(
        messageEntity = toMessageEntity(isParentMessage),
        from = user?.toUserEntity(),
        parent = parentMessage?.toParentMessageEntity(),
        attachments = attachments?.map { it.toAttachmentDb(id, tid, channelId) },
        reactions = userReactions?.map { it.toReactionDb() },
        reactionsScores = reactionTotals?.map { it.toReactionTotalEntity(id) },
        forwardingUser = forwardingDetails?.user?.toUserEntity(),
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
            userReactions = selfReactions?.map { it.toReaction() }?.toTypedArray(),
            reactionTotals = reactionsScores?.map { it.toReactionScore() }?.toTypedArray(),
            markerTotals = markerCount?.toTypedArray(),
            userMarkers = userMarkers?.toTypedArray(),
            mentionedUsers = mentionedUsers?.map {
                it.user?.toUser() ?: User(it.link.userId)
            }?.toTypedArray(),
            parentMessage = parent?.toSceytMessage(),
            replyCount = replyCount,
            displayCount = displayCount,
            autoDeleteDate = autoDeleteDate,
            forwardingDetails = forwardingDetailsDb?.toForwardingDetails(channelId, forwardingUser?.toUser())
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
    userMarkers = userMarkers?.toTypedArray(),
    mentionedUsers = mentionedUsers,
    parentMessage = null,
    replyCount = replyCount,
    displayCount = displayCount,
    autoDeleteDate = autoDeleteDate,
    forwardingDetails = forwardingDetailsDb?.toForwardingDetails(channelId, null)
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
            reactionsScores?.map { it.toReactionScore() }?.toTypedArray(),
            markerCount?.toTypedArray(),
            emptyArray(),
            emptyArray(),
            parent?.toSceytMessage()?.toMessage(),
            replyCount,
            messageEntity.displayCount,
            messageEntity.autoDeleteDate ?: 0L,
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
        userReactions = userReactions,
        reactionTotals = reactionTotals,
        markerTotals = markerTotals,
        userMarkers = userMarkers,
        mentionedUsers = mentionedUsers,
        parentMessage = parentMessage?.toSceytUiMessage(),
        replyCount = replyCount,
        displayCount = displayCount.toShort(),
        autoDeleteDate = autoDeleteDate,
        forwardingDetails = forwardingDetails
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
        userReactions,
        reactionTotals,
        markerTotals,
        userMarkers,
        mentionedUsers,
        parentMessage?.toMessage(),
        replyCount,
        displayCount,
        autoDeleteDate ?: 0L,
        ForwardingDetails(id, channelId, user, 0))
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
