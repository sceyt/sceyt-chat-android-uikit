package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["messageId", "channelId", "reaction_key", "fromId"], unique = true)])
data class ChatUserReactionEntity(
        @PrimaryKey
        val id: Long,
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(index = true)
        val channelId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        var key: String,
        val score: Int,
        val reason: String,
        val createdAt: Long,
        @ColumnInfo(index = true)
        val fromId: String?
)