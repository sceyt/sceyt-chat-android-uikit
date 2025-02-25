package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

internal const val MARKER_TABLE = "sceyt_marker_table"

@Entity(
    tableName = MARKER_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    primaryKeys = ["messageId", "name", "userId"])
data class MarkerEntity(
        @ColumnInfo(index = true)
        val messageId: Long,
        val userId: String,
        @ColumnInfo(index = true)
        val name: String,
        @ColumnInfo(index = true)
        val createdAt: Long = 0
)