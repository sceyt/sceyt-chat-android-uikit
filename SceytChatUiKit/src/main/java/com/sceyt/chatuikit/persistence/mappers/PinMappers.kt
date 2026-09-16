package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.PinDetails
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chat.models.message.PinnedMessage
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.data.models.messages.SceytPinDetails
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
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

internal fun PinType.toStoredPinScope() = when (this) {
    PinType.PERSONAL -> StoredPinScope.ForMe
    PinType.SHARED -> StoredPinScope.ForAll
}

internal fun StoredPinScope.toPinType() = when (this) {
    StoredPinScope.ForMe -> PinType.PERSONAL
    StoredPinScope.ForAll, StoredPinScope.Unspecified -> PinType.SHARED
}

internal fun PinnedMessageDb.toSceytPinnedMessage(): SceytPinnedMessage? {
    val message = message?.toSceytMessage() ?: return null
    return with(pinnedMessageEntity) {
        SceytPinnedMessage(
            id = serverPinId,
            channelId = channelId,
            messageId = messageId,
            messageTid = messageTid,
            scope = StoredPinScope.fromValue(pinScope).toPinType(),
            pinnedAt = pinnedAt,
            pinnedUntil = pinnedUntil,
            pinnedBy = this@toSceytPinnedMessage.pinnedBy?.toSceytUser(),
            message = message,
            syncState = PinSyncState.fromValue(syncState),
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
    pinScope = scope.toStoredPinScope().value,
    pinnedAt = pinnedAt,
    pinnedUntil = pinnedUntil?.takeIf { it > 0L },
    pinnedByUserId = pinnedBy?.id,
    messageCreatedAt = message.createdAt,
    serverPinId = id,
    syncState = PinSyncState.Synced.value,
    retryCount = 0,
    lastAttemptAt = 0L,
)
internal fun PinnedMessageEntity.toSceytPinDetails(): SceytPinDetails? {
    if (syncState == PinSyncState.PendingUnpin.value) return null
    return SceytPinDetails(
        isPinned = true,
        pinnedTill = pinnedUntil ?: 0L,
        pinType = StoredPinScope.fromValue(pinScope).toPinType(),
    )
}

internal fun StoredPinScope.toPinTypeOrdinal(): Int = when (this) {
    StoredPinScope.ForMe -> PinType.PERSONAL.ordinal
    StoredPinScope.ForAll, StoredPinScope.Unspecified ->
        PinType.SHARED.ordinal
}