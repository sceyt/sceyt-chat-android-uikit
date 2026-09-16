package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.PinDetails
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chat.models.message.PinnedMessage
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.data.models.messages.SceytPinDetails
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.entity.messages.PinScopeEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity

internal fun PinDetails.toSceytPinDetails() = SceytPinDetails(
    isPinned = isPinned,
    pinnedTill = pinnedTill,
    pinType = pinType,
)

internal fun SceytPinDetails.toPinDetails() = PinDetails(
    isPinned,
    pinnedTill,
    pinType.ordinal,
)

internal fun PinType.toPinScopeEntity() = when (this) {
    PinType.PERSONAL -> PinScopeEntity.ForMe
    PinType.SHARED -> PinScopeEntity.ForAll
}

internal fun PinScopeEntity.toPinType() = when (this) {
    PinScopeEntity.ForMe -> PinType.PERSONAL
    PinScopeEntity.ForAll, PinScopeEntity.Unspecified -> PinType.SHARED
}

internal fun PinSyncStateEntity.toPinSyncState() = when (this) {
    PinSyncStateEntity.Unspecified -> PinSyncState.Unspecified
    PinSyncStateEntity.Synced -> PinSyncState.Synced
    PinSyncStateEntity.PendingPin -> PinSyncState.PendingPin
    PinSyncStateEntity.PendingUnpin -> PinSyncState.PendingUnpin
}

internal fun PinnedMessageDb.toSceytPinnedMessage(): SceytPinnedMessage? {
    val message = message?.toSceytMessage() ?: return null
    return with(pinnedMessageEntity) {
        SceytPinnedMessage(
            id = serverPinId,
            channelId = channelId,
            messageId = messageId,
            messageTid = messageTid,
            scope = PinScopeEntity.fromValue(pinScope).toPinType(),
            pinnedAt = pinnedAt,
            pinnedUntil = pinnedUntil,
            pinnedBy = this@toSceytPinnedMessage.pinnedBy?.toSceytUser(),
            message = message,
            syncState = PinSyncStateEntity.fromValue(syncState).toPinSyncState(),
            retryCount = retryCount,
        )
    }
}

internal fun PinnedMessage.toSceytPinnedMessage(channelId: Long): SceytPinnedMessage? {
    val sdkMessage = message ?: return null
    val uiMessage = sdkMessage.toSceytUiMessage()
    return SceytPinnedMessage(
        id = id,
        channelId = channelId,
        messageId = sdkMessage.id,
        messageTid = getTid(sdkMessage.id, uiMessage.tid, uiMessage.incoming),
        scope = sdkMessage.pinDetails?.pinType ?: PinType.SHARED,
        pinnedAt = null,
        pinnedUntil = sdkMessage.pinDetails?.pinnedTill,
        pinnedBy = pinnedBy?.toSceytUser(),
        message = uiMessage,
        syncState = PinSyncState.Synced,
        retryCount = 0,
    )
}

internal fun SceytPinnedMessage.toPinnedMessageEntity(
    channelId: Long,
    messageTid: Long,
) = PinnedMessageEntity(
    messageTid = messageTid,
    channelId = channelId,
    messageId = message.id,
    pinScope = scope.toPinScopeEntity().value,
    pinnedAt = pinnedAt,
    pinnedUntil = pinnedUntil?.takeIf { it > 0L },
    pinnedByUserId = pinnedBy?.id,
    messageCreatedAt = message.createdAt,
    serverPinId = id,
    syncState = PinSyncStateEntity.Synced.value,
    retryCount = 0,
    lastAttemptAt = 0L,
)
internal fun PinnedMessageEntity.toSceytPinDetails(): SceytPinDetails? {
    if (syncState == PinSyncStateEntity.PendingUnpin.value) return null
    return SceytPinDetails(
        isPinned = true,
        pinnedTill = pinnedUntil ?: 0L,
        pinType = PinScopeEntity.fromValue(pinScope).toPinType(),
    )
}

internal fun PinScopeEntity.toPinTypeOrdinal(): Int = when (this) {
    PinScopeEntity.ForMe -> PinType.PERSONAL.ordinal
    PinScopeEntity.ForAll, PinScopeEntity.Unspecified ->
        PinType.SHARED.ordinal
}