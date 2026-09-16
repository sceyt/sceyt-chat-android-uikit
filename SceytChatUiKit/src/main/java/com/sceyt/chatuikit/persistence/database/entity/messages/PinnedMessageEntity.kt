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
        const val UNKNOWN_SERVER_PIN_ID = Long.MAX_VALUE
    }
}