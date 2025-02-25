package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

internal const val USER_CHAT_LINK_TABLE = "sceyt_user_chat_link_table"

@Entity(
    tableName = USER_CHAT_LINK_TABLE,
    indices = [Index(value = ["chat_id", "user_id"], unique = true, name = "uniqueUserInChat")])
data class UserChatLinkEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String,
        @ColumnInfo(name = "chat_id", index = true)
        val chatId: Long,
        val role: String
)