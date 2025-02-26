package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity

internal const val PENDING_MARKER_TABLE = "sceyt_pending_marker_table"

@Entity(
    tableName = PENDING_MARKER_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId", "name", unique = true)]
)
internal data class PendingMarkerEntity(
        @PrimaryKey(autoGenerate = true)
        val primaryKey: Int = 0,
        val channelId: Long,
        val messageId: Long,
        val name: String
)