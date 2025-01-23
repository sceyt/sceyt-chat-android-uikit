package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.*

@Entity(foreignKeys = [
    ForeignKey(
        entity = MessageEntity::class,
        parentColumns = ["message_id"],
        childColumns = ["messageId"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )],
    indices = [Index("messageId", "reaction_key", unique = true)])
data class ReactionTotalEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        val key: String,
        val score: Int,
        val count: Long
)
