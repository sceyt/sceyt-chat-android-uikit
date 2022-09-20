package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("messageId", "reaction_key", "fromId", unique = true)])
data class ReactionEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        var key: String,
        val score: Int,
        val reason: String,
        val updateAt: Long,
        @ColumnInfo(index = true)
        val fromId: String?
)
