package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
        ForeignKey(
                entity = MessageEntity::class,
                parentColumns = ["message_id"],
                childColumns = ["messageId"],
                onDelete = ForeignKey.CASCADE
        )
],
tableName = "AutoDeletedMessages")
class AutoDeletedMessageEntity(
        @PrimaryKey
        val messageId: Long,
        val channelId: Long,
        val autoDeleteAt: Long
)