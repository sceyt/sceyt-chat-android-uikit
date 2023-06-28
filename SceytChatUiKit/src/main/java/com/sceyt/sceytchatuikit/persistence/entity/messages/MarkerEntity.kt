package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
    ForeignKey(
        entity = MessageEntity::class,
        parentColumns = ["message_id"],
        childColumns = ["messageId"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )
], indices = [Index("messageId", "name", "userId", unique = true)])
data class MarkerEntity(
        @ColumnInfo(index = true)
        var messageId: Long,
        var userId: String,
        var name: String,
        var createdAt: Long = 0,
        @PrimaryKey(autoGenerate = true)
        val primaryKey: Long = 0
)