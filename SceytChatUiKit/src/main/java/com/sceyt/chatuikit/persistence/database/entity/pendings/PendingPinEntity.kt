package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_PIN_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity

@Entity(
    tableName = PENDING_PIN_TABLE,
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
        Index(value = ["messageTid"], unique = true),
        Index(value = ["channelId"])
    ]
)
internal data class PendingPinEntity(
    val messageTid: Long,
    val channelId: Long,
    val messageId: Long,
    val isPin: Boolean,
    val pinScope: Int,
    val createdAt: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)