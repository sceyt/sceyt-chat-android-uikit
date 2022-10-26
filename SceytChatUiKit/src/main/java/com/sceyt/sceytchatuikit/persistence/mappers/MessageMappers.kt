package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toMessage
import com.sceyt.sceytchatuikit.data.toSceytUiMessage
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ParentMessageDb
import java.util.*

fun SceytMessage.toMessageEntity() = MessageEntity(
    id = id,
    tid = getTid(id, tid),
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
    deliveryStatus = deliveryStatus,
    state = state,
    fromId = from?.id,
    parentId = parent?.id,
    replyInThread = replyInThread,
    replyCount = replyCount
)

private fun getTid(msgId: Long, tid: Long): Long {
    return if (tid == 0L)
        msgId
    else tid
}

fun Message.toMessageEntity() = MessageEntity(
    id = id,
    tid = getTid(id, tid),
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
    deliveryStatus = deliveryStatus,
    state = state,
    fromId = from?.id,
    parentId = parent?.id,
    replyInThread = replyInThread,
    replyCount = replyCount
)


fun SceytMessage.toMessageDb(): MessageDb {
    val tid = getTid(id, tid)
    return MessageDb(
        messageEntity = toMessageEntity(),
        from = from?.toUserEntity(),
        parent = parent?.toParentMessageEntity(),
        attachments = attachments?.map { it.toAttachmentEntity(id, tid) },
        lastReactions = lastReactions?.map { it.toReactionDb(id) },
        reactionsScores = reactionScores?.map { it.toReactionScoreEntity(id) }
    )
}

fun Message.toMessageDb(): MessageDb {
    val tid = getTid(id, tid)
    return MessageDb(
        messageEntity = toMessageEntity(),
        from = from?.toUserEntity(),
        parent = parent?.toSceytUiMessage()?.toParentMessageEntity(),
        attachments = attachments?.map { it.toAttachmentEntity(id, tid) },
        lastReactions = lastReactions?.map { it.toReactionDb(id) },
        reactionsScores = reactionScores?.map { it.toReactionScoreEntity(id) }
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
            deliveryStatus = deliveryStatus,
            state = state,
            from = from?.toUser(),
            attachments = attachments?.map { it.toAttachment() }?.toTypedArray(),
            lastReactions = lastReactions?.map { it.toReaction() }?.toTypedArray(),
            reactionScores = reactionsScores?.map { it.toReactionScore() }?.toTypedArray(),
            parent = parent?.toSceytMessage(),
            replyInThread = replyInThread,
            replyCount = replyCount
        )
    }
}

fun ParentMessageDb.toSceytMessage(): SceytMessage {
    return messageEntity.toSceytMessage().apply {
        this.from = this@toSceytMessage.from?.toUser()
    }
}

fun SceytMessage.toParentMessageEntity(): ParentMessageDb {
    return ParentMessageDb(toMessageEntity(), from?.toUserEntity())
}

private fun MessageEntity.toSceytMessage() = SceytMessage(
    id ?: 0,
    tid,
    channelId,
    to,
    body,
    type,
    metadata,
    createdAt,
    Date(updatedAt),
    incoming,
    receipt,
    isTransient,
    silent,
    deliveryStatus,
    state,
    null,
    emptyArray(),
    emptyArray(),
    emptyArray(),
    emptyArray(),
    emptyArray(),
    emptyArray(),
    emptyArray(),
    null,
    replyInThread,
    replyCount
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
            deliveryStatus,
            state,
            from?.toUser(),
            attachments?.map { it.toSdkAttachment() }?.toTypedArray(),
            emptyArray(),
            emptyArray(),
            emptyArray(),
            emptyArray(),
            emptyArray(),
            emptyArray(),
            parent?.toSceytMessage()?.toMessage(),
            replyInThread,
            replyCount
        )
    }
}