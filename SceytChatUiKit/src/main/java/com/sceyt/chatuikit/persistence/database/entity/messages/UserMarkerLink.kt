package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(foreignKeys = [
    ForeignKey(
        entity = MessageEntity::class,
        parentColumns = ["message_id"],
        childColumns = ["message_id"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )
], primaryKeys = ["message_id", "markerId"])
data class UserMarkerLink(
        @ColumnInfo(name = "message_id", index = true)
        val messageId: Long,
        @ColumnInfo(index = true)
        val markerId: Long
)