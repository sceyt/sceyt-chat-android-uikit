package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

internal const val AUTO_DELETE_MESSAGES_TABLE = "sceyt_auto_delete_messages_table"

@Entity(
    tableName = AUTO_DELETE_MESSAGES_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE
        )
    ])
internal class AutoDeleteMessageEntity(
        @PrimaryKey
        val messageTid: Long,
        val channelId: Long,
        val autoDeleteAt: Long
)