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

fun SceytMessage.toMessageEntity() = MessageEntity(
    tid = getTid(id, tid, incoming),
    id = id,
    channelId = channelId,
    to = to,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = updatedAt.time,
    incoming = incoming,
    receipt = receipt,
    isTransient = isTransient,
    silent = silent,
    direct = direct,
    deliveryStatus = deliveryStatus,
    state = state,
    fromId = from?.id,
    markerCount = markerCount?.toList(),
    mentionedUsersIds = mentionedUsers?.map { it.id },
    selfMarkers = selfMarkers?.toList(),
    parentId = if (parent?.id == 0L) null else parent?.id,
    replyInThread = replyInThread,
    replyCount = replyCount,
    displayCount = displayCount,
    forwardingDetailsDb = forwardingDetails?.toForwardingDetailsDb()
)

fun getTid(msgId: Long, tid: Long, incoming: Boolean): Long {
    return if (incoming)
        msgId
    else tid
}


fun SceytMessage.toMessageDb(): MessageDb {
    val tid = getTid(id, tid, incoming)
    return MessageDb(
        messageEntity = toMessageEntity(),
        from = from?.toUserEntity(),
        parent = parent?.toParentMessageEntity(),
        attachments = attachments?.map { it.toAttachmentDb(id, tid, channelId) },
        reactions = selfReactions?.map { it.toReactionDb() },
        reactionsScores = reactionScores?.map { it.toReactionScoreEntity(id) },
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
            to = to,
            body = body,
            type = type,
            metadata = metadata,
            createdAt = createdAt,
            updatedAt = Date(updatedAt),
            incoming = incoming,
            receipt = receipt,
            isTransient = isTransient,
            silent = silent,
            direct = direct,
            deliveryStatus = deliveryStatus,
            state = state,
            from = from?.toUser(),
            attachments = attachments?.map { it.toAttachment() }?.toTypedArray(),
            selfReactions = selfReactions?.map { it.toReaction() }?.toTypedArray(),
            reactionScores = reactionsScores?.map { it.toReactionScore() }?.toTypedArray(),
            markerCount = markerCount?.toTypedArray(),
            selfMarkers = selfMarkers?.toTypedArray(),
            mentionedUsers = mentionedUsers?.map {
                it.user?.toUser() ?: User(it.link.userId)
            }?.toTypedArray(),
            parent = parent?.toSceytMessage(),
            replyInThread = replyInThread,
            replyCount = replyCount,
            displayCount = displayCount,
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
    return ParentMessageDb(toMessageEntity(), from?.toUserEntity(), attachments?.map {
        it.toAttachmentDb(id, getTid(id, tid, incoming), channelId)
    }, null)
}

private fun MessageEntity.parentMessageToSceytMessage(attachments: Array<SceytAttachment>?,
                                                      from: User?, mentionedUsers: Array<User>?) = SceytMessage(
    id = id ?: 0,
    tid = tid,
    channelId = channelId,
    to = to,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt,
    updatedAt = Date(updatedAt),
    incoming = incoming,
    receipt = receipt,
    isTransient = isTransient,
    silent = silent,
    direct = direct,
    deliveryStatus = deliveryStatus,
    state = state,
    from = from,
    attachments = attachments,
    selfReactions = emptyArray(),
    reactionScores = emptyArray(),
    markerCount = markerCount?.toTypedArray(),
    selfMarkers = selfMarkers?.toTypedArray(),
    mentionedUsers = mentionedUsers,
    parent = null,
    replyInThread = replyInThread,
    replyCount = replyCount,
    displayCount = displayCount,
    forwardingDetails = forwardingDetailsDb?.toForwardingDetails(channelId, null)
)

fun MessageDb.toMessage(): Message {
    with(messageEntity) {
        return Message(
            id ?: 0,
            tid,
            channelId,
            to,
            body,
            type,
            metadata,
            createdAt,
            updatedAt,
            incoming,
            receipt,
            isTransient,
            silent,
            direct,
            deliveryStatus,
            state,
            from?.toUser(),
            attachments?.map { it.toSdkAttachment() }?.toTypedArray(),
            selfReactions?.map { it.toReaction() }?.toTypedArray(),
            reactionsScores?.map { it.toReactionScore() }?.toTypedArray(),
            markerCount?.toTypedArray(),
            emptyArray(),
            emptyArray(),
            parent?.toSceytMessage()?.toMessage(),
            replyInThread,
            replyCount,
            messageEntity.displayCount,
            ForwardingDetails(id ?: 0, channelId, from?.toUser(), 0)
        )
    }
}


fun Message.toSceytUiMessage(isGroup: Boolean? = null): SceytMessage {
    val tid = getTid(id, tid, incoming)
    return SceytMessage(
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
        direct = isDirect,
        deliveryStatus = deliveryStatus,
        state = state,
        from = from,
        attachments = attachments?.map {
            val transferState: TransferState
            val progress: Float
            if (it.filePath != null) {
                transferState = TransferState.Downloaded
                progress = 100f
            } else {
                transferState = TransferState.PendingDownload
                progress = 0f
            }
            it.toSceytAttachment(tid, transferState, progress)
        }?.toTypedArray(),
        selfReactions = selfReactions,
        reactionScores = reactionScores,
        markerCount = markerCount,
        selfMarkers = selfMarkers,
        mentionedUsers = mentionedUsers,
        parent = parent?.toSceytUiMessage(),
        replyInThread = replyInThread,
        replyCount = replyCount,
        displayCount = displayCount.toShort(),
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
        direct,
        deliveryStatus,
        state,
        from,
        attachments?.map { it.toAttachment() }?.toTypedArray(),
        selfReactions,
        reactionScores,
        markerCount,
        selfMarkers,
        mentionedUsers,
        parent?.toMessage(),
        replyInThread,
        replyCount,
        displayCount,
        ForwardingDetails(id, channelId, from, 0))
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
