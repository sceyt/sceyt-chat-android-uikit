package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

internal const val MENTION_USER_MESSAGE_LINK_TABLE = "sceyt_mention_user_message_link_table"

@Entity(
    tableName = MENTION_USER_MESSAGE_LINK_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ], primaryKeys = ["messageTid", "user_id"])
data class MentionUserMessageLinkEntity(
        @ColumnInfo(index = true)
        val messageTid: Long,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String
)