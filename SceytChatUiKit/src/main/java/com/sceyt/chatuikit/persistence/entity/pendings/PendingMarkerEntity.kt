package com.sceyt.chatuikit.persistence.entity.pendings

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.entity.messages.MessageEntity

@Entity(tableName = "PendingMarker",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId", "name", unique = true)]
)
data class PendingMarkerEntity(
        @PrimaryKey(autoGenerate = true)
        val primaryKey: Int = 0,
        val channelId: Long,
        val messageId: Long,
        val name: String
)