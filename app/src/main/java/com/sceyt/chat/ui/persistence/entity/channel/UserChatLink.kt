package com.sceyt.chat.ui.persistence.entity.channel

import androidx.room.*

@Entity(indices = [
    Index(value = arrayOf("chat_id")),
    Index(value = arrayOf("user_id")),
    Index(value = ["chat_id", "user_id"], unique = true, name = "uniqueUserInChat")],
    foreignKeys = [ForeignKey(
        entity = ChannelEntity::class,
        parentColumns = arrayOf("chat_id"),
        childColumns = arrayOf("chat_id"),
        onDelete = ForeignKey.CASCADE)])

data class UserChatLink(
        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        @ColumnInfo(name = "user_id")
        var userId: String,
        @ColumnInfo(name = "chat_id")
        var chatId: Long,
        var role: String,
)