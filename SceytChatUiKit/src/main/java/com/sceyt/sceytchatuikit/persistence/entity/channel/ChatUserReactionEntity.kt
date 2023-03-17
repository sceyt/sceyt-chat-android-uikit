package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
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
        val updateAt: Long,
        @ColumnInfo(index = true)
        val fromId: String?
)