package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toAttachment
import com.sceyt.sceytchatuikit.data.toSceytAttachment
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ParentMessageDb
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
    selfMarkers = selfMarkers?.toList(),
    parentId = if (parent?.id == 0L) null else parent?.id,
    replyInThread = replyInThread,
    replyCount = replyCount,
    displayCount = displayCount
)

fun getTid(msgId: Long, tid: Long, incoming: Boolean): Long {
    return if (incoming)
        msgId
    else tid
}

fun Message.toMessageEntity() = MessageEntity(
    tid = getTid(id, tid, incoming),
    id = id,
    channelId = channelId,
    to = to,
    body = body,
    type = type,
    metadata = metadata,
    createdAt = createdAt.time,
    updatedAt = updatedAt.time,
    incoming = incoming,
    receipt = receipt,
    isTransient = isTransient,
    silent = silent,
    direct = isDirect,
    deliveryStatus = deliveryStatus,
    state = state,
    fromId = from?.id,
    markerCount = markerCount?.toList(),
    selfMarkers = selfMarkers?.toList(),
    parentId = if (parent?.id == 0L) null else parent?.id,
    replyInThread = replyInThread,
    replyCount = replyCount,
    displayCount = displayCount.toShort()
)


fun SceytMessage.toMessageDb(): MessageDb {
    val tid = getTid(id, tid, incoming)
    return MessageDb(
        messageEntity = toMessageEntity(),
        from = from?.toUserEntity(),
        parent = parent?.toParentMessageEntity(),
        attachments = attachments?.map { it.toAttachmentDb(id, tid) },
        lastReactions = lastReactions?.map { it.toReactionDb(id) },
        reactionsScores = reactionScores?.map { it.toReactionScoreEntity(id) },
        attachmentPayLoadEntity = toAttachmentPayLoad()
    )
}

fun SceytMessage.toAttachmentPayLoad(): AttachmentPayLoadEntity? {
    val tid = getTid(id, tid, incoming)
    val attachment = attachments?.getOrNull(0) ?: return null
    return AttachmentPayLoadEntity(
        messageTid = tid,
        transferState = attachment.transferState,
        progressPercent = attachment.progressPercent,
        url = attachment.url,
        filePath = attachment.filePath
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
            lastReactions = lastReactions?.map { it.toReaction() }?.toTypedArray(),
            reactionScores = reactionsScores?.map { it.toReactionScore() }?.toTypedArray(),
            markerCount = markerCount?.toTypedArray(),
            selfMarkers = selfMarkers?.toTypedArray(),
            parent = parent?.toSceytMessage(),
            replyInThread = replyInThread,
            replyCount = replyCount,
            displayCount = displayCount
        )
    }
}

fun ParentMessageDb.toSceytMessage(): SceytMessage {
    return messageEntity.toSceytMessage().apply {
        this.from = this@toSceytMessage.from?.toUser()
        this.attachments = this@toSceytMessage.attachments?.map { it.toAttachment() }?.toTypedArray()
    }
}

fun SceytMessage.toParentMessageEntity(): ParentMessageDb {
    return ParentMessageDb(toMessageEntity(), from?.toUserEntity(), attachments?.map {
        it.toAttachmentDb(id, getTid(id, tid, incoming))
    })
}

private fun MessageEntity.toSceytMessage() = SceytMessage(
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
    from = null,
    attachments = emptyArray(),
    lastReactions = emptyArray(),
    selfReactions = emptyArray(),
    reactionScores = emptyArray(),
    markerCount = markerCount?.toTypedArray(),
    selfMarkers = selfMarkers?.toTypedArray(),
    mentionedUsers = emptyArray(),
    parent = null,
    replyInThread = replyInThread,
    replyCount = replyCount,
    displayCount = displayCount
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
            lastReactions?.map { it.toReaction() }?.toTypedArray(),
            emptyArray(),
            reactionsScores?.map { it.toReactionScore() }?.toTypedArray(),
            markerCount?.toTypedArray(),
            emptyArray(),
            emptyArray(),
            parent?.toSceytMessage()?.toMessage(),
            replyInThread,
            replyCount,
            messageEntity.displayCount
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
        attachments = attachments?.map { it.toSceytAttachment(tid) }?.toTypedArray(),
        lastReactions = lastReactions,
        selfReactions = selfReactions,
        reactionScores = reactionScores,
        markerCount = markerCount,
        selfMarkers = selfMarkers,
        mentionedUsers = mentionedUsers,
        parent = parent?.toSceytUiMessage(),
        replyInThread = replyInThread,
        replyCount = replyCount,
        displayCount = displayCount.toShort()
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
        lastReactions,
        selfReactions,
        reactionScores,
        markerCount,
        selfMarkers,
        mentionedUsers,
        parent?.toMessage(),
        replyInThread,
        replyCount,
        displayCount)
}
