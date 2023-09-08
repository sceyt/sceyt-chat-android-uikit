package com.sceyt.sceytchatuikit.persistence.entity.pendings

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity

@Entity(tableName = "pendingReaction",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )],
    indices = [Index("messageId", "reaction_key", unique = true)])
data class PendingReactionEntity(
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        var key: String,
        var score: Int,
        var count: Long,
        val channelId: Long,
        var isAdd: Boolean,
        var createdAt: Long,
        @ColumnInfo(defaultValue = "false")
        var incomingMsg: Boolean,
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0
)
