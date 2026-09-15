package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PINNED_MESSAGE_TABLE

@Entity(
    tableName = PINNED_MESSAGE_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index(value = ["messageTid", "channelId"], unique = true),
        Index(value = ["channelId"]),
    ]
)
internal data class PinnedMessageEntity(
    @PrimaryKey
    val messageTid: Long,
    val channelId: Long,
    val messageId: Long,
    val pinScope: Int,
    val pinnedAt: Long?,
    val pinnedUntil: Long?,
    val pinnedByUserId: String?,
    val messageCreatedAt: Long?,
    val serverPinId: Long,
    val syncState: Int,
    val retryCount: Int,
    val lastAttemptAt: Long,
) {

    companion object {
        /**
         * Sorts to the newest-pin end under both ascending and descending order, so an
         * optimistic pin appends rather than jumping to the head of the banner. Deliberately
         * not 0, which is a legitimate "written before the server pin API" value.
         */
        const val UNKNOWN_SERVER_PIN_ID = Long.MAX_VALUE
    }
}

internal enum class PinScopeEntity(val value: Int) {
    Unspecified(0),
    ForMe(1),
    ForAll(2);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: Unspecified
    }
}

internal object PinSyncStates {
    const val UNSPECIFIED = 0
    const val SYNCED = 1
    const val PENDING_PIN = 2
    const val PENDING_UNPIN = 3
}

internal enum class PinSyncStateEntity(val value: Int) {
    Unspecified(PinSyncStates.UNSPECIFIED),
    Synced(PinSyncStates.SYNCED),
    PendingPin(PinSyncStates.PENDING_PIN),
    PendingUnpin(PinSyncStates.PENDING_UNPIN);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: Unspecified
    }
}

/**
 * The ordinal the pin mirror stores, matching `PinDetails.PinType` as written by
 * `MessageConverter.pinTypeToInt`.
 */
internal fun PinScopeEntity.toPinTypeOrdinal(): Int = when (this) {
    PinScopeEntity.ForMe -> com.sceyt.chat.models.message.PinDetails.PinType.PERSONAL.ordinal
    PinScopeEntity.ForAll, PinScopeEntity.Unspecified ->
        com.sceyt.chat.models.message.PinDetails.PinType.SHARED.ordinal
}
