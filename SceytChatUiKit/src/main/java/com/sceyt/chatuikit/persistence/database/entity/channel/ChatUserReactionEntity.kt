package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

internal const val CHAT_USER_REACTION_TABLE = "sceyt_chat_user_reaction_table"

@Entity(
    tableName = CHAT_USER_REACTION_TABLE,
    indices = [Index(value = ["messageId", "channelId", "reaction_key", "fromId"], unique = true)]
)
internal data class ChatUserReactionEntity(
        @PrimaryKey
        val id: Long,
        @ColumnInfo(index = true)
        val messageId: Long,
        @ColumnInfo(index = true)
        val channelId: Long,
        @ColumnInfo(name = "reaction_key", index = true)
        val key: String,
        val score: Int,
        val reason: String,
        val createdAt: Long,
        @ColumnInfo(index = true)
        val fromId: String?
)