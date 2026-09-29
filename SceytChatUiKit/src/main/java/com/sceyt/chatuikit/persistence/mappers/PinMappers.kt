package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.message.PinDetails
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chat.models.message.PinnedMessage
import com.sceyt.chatuikit.data.models.messages.SceytPinDetails
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinDb
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity

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

internal fun pinDetailsOf(
    confirmed: PinnedMessageEntity?,
    pending: PendingPinEntity?,
): SceytPinDetails? = when {
    pending?.isPin == false -> null
    confirmed != null -> SceytPinDetails(
        isPinned = true,
        pinnedTill = confirmed.pinnedUntil ?: 0L,
        pinType = StoredPinScope.fromValue(confirmed.pinScope).toPinType(),
    )

    pending != null -> SceytPinDetails(
        isPinned = true,
        pinnedTill = 0L,
        pinType = StoredPinScope.fromValue(pending.pinScope).toPinType(),
    )

    else -> null
}

internal fun mergePins(
    confirmed: List<SceytPinnedMessage>,
    pending: List<PendingPinDb>,
): List<SceytPinnedMessage> {
    val pendingByTid = pending.associateBy { it.pendingPin.messageTid }
    val confirmedTids = confirmed.mapTo(HashSet()) { it.messageTid }
    return confirmed.filter { pendingByTid[it.messageTid]?.pendingPin?.isPin != false } +
            pending.filter { it.pendingPin.isPin && it.pendingPin.messageTid !in confirmedTids }
                .mapNotNull { it.toSceytPinnedMessage() }
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
        )
    }
}

internal fun PendingPinDb.toSceytPinnedMessage(): SceytPinnedMessage? {
    val message = message?.toSceytMessage() ?: return null
    return with(pendingPin) {
        SceytPinnedMessage(
            id = 0L,
            channelId = channelId,
            messageId = messageId,
            messageTid = messageTid,
            scope = StoredPinScope.fromValue(pinScope).toPinType(),
            pinnedAt = createdAt,
            pinnedUntil = null,
            pinnedBy = null,
            message = message,
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
)