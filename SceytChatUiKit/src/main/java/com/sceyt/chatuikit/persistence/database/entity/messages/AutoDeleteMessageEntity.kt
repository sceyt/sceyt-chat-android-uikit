package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
        ForeignKey(
                entity = MessageEntity::class,
                parentColumns = ["tid"],
                childColumns = ["messageTid"],
                onDelete = ForeignKey.CASCADE
        )
],
tableName = "AutoDeleteMessages")
class AutoDeleteMessageEntity(
        @PrimaryKey
        val messageTid: Long,
        val channelId: Long,
        val autoDeleteAt: Long
)