package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity

internal const val PENDING_REACTION_TABLE = "sceyt_pending_reaction_table"

@Entity(
    tableName = PENDING_REACTION_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )],
    indices = [Index("messageId", "reaction_key", unique = true)])
internal data class PendingReactionEntity(
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        val key: String,
        val score: Int,
        @ColumnInfo(defaultValue = "")
        val reason: String,
        @ColumnInfo(defaultValue = "false")
        val enforceUnique: Boolean,
        val count: Long,
        val channelId: Long,
        val isAdd: Boolean,
        val createdAt: Long,
        @ColumnInfo(defaultValue = "false")
        val incomingMsg: Boolean,
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0
)
