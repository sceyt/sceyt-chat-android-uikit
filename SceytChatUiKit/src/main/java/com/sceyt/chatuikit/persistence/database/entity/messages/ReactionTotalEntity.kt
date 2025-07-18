package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.REACTION_TOTAL_TABLE

@Entity(
    tableName = REACTION_TOTAL_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["message_id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true
        )],
    indices = [Index("messageId", "reaction_key", unique = true)])
internal data class ReactionTotalEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        val key: String,
        val score: Int,
        val count: Long
)
