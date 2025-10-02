package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.AUTO_DELETE_MESSAGES_TABLE

@Entity(
    tableName = AUTO_DELETE_MESSAGES_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ])
internal class AutoDeleteMessageEntity(
        @PrimaryKey
        val messageTid: Long,
        val channelId: Long,
        val autoDeleteAt: Long
)