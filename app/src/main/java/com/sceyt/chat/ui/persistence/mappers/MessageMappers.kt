package com.sceyt.chat.ui.persistence.mappers

import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.persistence.entity.messages.MessageDb
import com.sceyt.chat.ui.persistence.entity.messages.MessageEntity
import java.util.*

fun SceytMessage.toMessageEntity() = MessageEntity(
    id = id,
    tid = tid,
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


fun Message.toMessageEntity() = MessageEntity(
    id = id,
    tid = tid,
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


fun SceytMessage.toMessageDb() = MessageDb(
    messageEntity = toMessageEntity(),
    from = from?.toUserEntity(),
    parent = parent?.toMessageEntity(),
    attachments = attachments?.map { it.toAttachmentEntity(id) }
)

fun Message.toMessageDb() = MessageDb(
    messageEntity = toMessageEntity(),
    from = from?.toUserEntity(),
    parent = parent?.toMessageEntity(),
    attachments = attachments?.map { it.toAttachmentEntity(id) }
)

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
            parent = parent?.toSceytMessage(),
            replyInThread = replyInThread,
            replyCount = replyCount
        )
    }
}

fun MessageEntity.toSceytMessage() = SceytMessage(
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
