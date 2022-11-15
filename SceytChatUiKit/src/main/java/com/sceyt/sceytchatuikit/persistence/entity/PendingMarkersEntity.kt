package com.sceyt.sceytchatuikit.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity

@Entity(tableName = "PendingMarkers",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId", "status", unique = true)]
)
data class PendingMarkersEntity(
        @PrimaryKey(autoGenerate = true)
        val primaryKey: Int = 0,
        val channelId: Long,
        val messageId: Long,
        val status: SelfMarkerTypeEnum
)