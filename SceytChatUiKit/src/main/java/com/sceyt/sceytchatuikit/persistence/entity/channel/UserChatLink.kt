package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["chat_id", "user_id"], unique = true, name = "uniqueUserInChat")])
data class UserChatLink(
        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        @ColumnInfo(name = "user_id", index = true)
        var userId: String,
        @ColumnInfo(name = "chat_id", index = true)
        var chatId: Long,
        var role: String,
)