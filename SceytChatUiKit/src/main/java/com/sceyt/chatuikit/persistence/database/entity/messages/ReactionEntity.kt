package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

internal const val REACTION_TABLE = "sceyt_reaction_table"

@Entity(
    tableName = REACTION_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ], indices = [Index("messageId", "reaction_key", "fromId", unique = true)])
data class ReactionEntity(
        @PrimaryKey
        val id: Long,
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        val key: String,
        val score: Int,
        val reason: String,
        val createdAt: Long,
        @ColumnInfo(index = true)
        val fromId: String?
)
