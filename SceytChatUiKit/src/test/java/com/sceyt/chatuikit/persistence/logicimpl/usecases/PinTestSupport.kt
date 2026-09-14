package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPinDetails
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity

/**
 * Shared fixtures for the pin use-case tests. Real entities rather than mocks, because the
 * mappers under test are extension functions and cannot be stubbed.
 */
internal fun messageEntity(
    id: Long = 42L,
    tid: Long = 42L,
    channelId: Long = 7L,
    createdAt: Long = 1_000L,
    deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.Sent,
    isTransient: Boolean = false,
    viewOnce: Boolean = false,
    autoDeleteAt: Long? = null,
    state: MessageState = MessageState.Unmodified,
) = MessageEntity(
    tid = tid,
    id = id.takeIf { it != 0L },
    channelId = channelId,
    body = "body",
    type = "text",
    metadata = null,
    createdAt = createdAt,
    updatedAt = 0L,
    incoming = false,
    isTransient = isTransient,
    silent = false,
    viewOnce = viewOnce,
    deliveryStatus = deliveryStatus,
    state = state,
    fromId = null,
    markerCount = null,
    mentionedUsersIds = null,
    parentId = null,
    replyCount = 0L,
    displayCount = 0,
    autoDeleteAt = autoDeleteAt,
    forwardingDetailsDb = null,
    bodyAttribute = null,
    disableMentionsCount = false,
    pinDetails = null,
    unList = false,
)

internal fun messageDb(entity: MessageEntity = messageEntity()) = MessageDb(
    messageEntity = entity,
    from = null,
    parent = null,
    attachments = null,
    userMarkers = null,
    reactions = null,
    reactionsTotals = null,
    pendingReactions = null,
    forwardingUser = null,
    mentionedUsers = null,
    poll = null,
    pinnedMessage = null,
)

internal fun pinnedEntity(
    messageTid: Long = 42L,
    channelId: Long = 7L,
    messageId: Long = 42L,
    syncState: Int,
    serverPinId: Long = 1L,
    pinScope: Int = 2,
    pinnedUntil: Long? = null,
) = PinnedMessageEntity(
    messageTid = messageTid,
    channelId = channelId,
    messageId = messageId,
    pinScope = pinScope,
    pinnedAt = 0L,
    pinnedUntil = pinnedUntil,
    pinnedByUserId = null,
    messageCreatedAt = 0L,
    serverPinId = serverPinId,
    syncState = syncState,
    retryCount = 0,
    lastAttemptAt = 0L,
)

internal fun sceytMessage(
    id: Long = 42L,
    tid: Long = 42L,
    channelId: Long = 7L,
    incoming: Boolean = false,
    isTransient: Boolean = false,
    viewOnce: Boolean = false,
    autoDeleteAt: Long? = null,
    state: MessageState = MessageState.Unmodified,
    pinDetails: SceytPinDetails? = null,
) = SceytMessage(
    id = id,
    tid = tid,
    channelId = channelId,
    body = "body",
    type = "text",
    metadata = null,
    createdAt = 1_000L,
    updatedAt = 0L,
    incoming = incoming,
    isTransient = isTransient,
    silent = false,
    viewOnce = viewOnce,
    deliveryStatus = MessageDeliveryStatus.Sent,
    state = state,
    user = null,
    attachments = null,
    userReactions = null,
    reactionTotals = null,
    markerTotals = null,
    userMarkers = null,
    mentionedUsers = null,
    parentMessage = null,
    replyCount = 0L,
    displayCount = 0,
    autoDeleteAt = autoDeleteAt,
    forwardingDetails = null,
    pendingReactions = null,
    bodyAttributes = null,
    disableMentionsCount = false,
    poll = null,
    pinDetails = pinDetails,
)

internal fun sceytPinnedMessage(
    id: Long = 500L,
    channelId: Long = 7L,
    message: SceytMessage = sceytMessage(channelId = channelId),
    messageTid: Long = message.tid,
    scope: PinType = PinType.SHARED,
    pinnedUntil: Long? = null,
) = SceytPinnedMessage(
    id = id,
    channelId = channelId,
    messageId = message.id,
    messageTid = messageTid,
    scope = scope,
    pinnedAt = 900L,
    pinnedUntil = pinnedUntil,
    pinnedBy = null,
    message = message,
    syncState = PinSyncState.Synced,
    retryCount = 0,
)
