package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

internal const val DRAFT_MESSAGE_USER_LINK_TABLE = "sceyt_draft_message_user_link_table"

@Entity(
    tableName = DRAFT_MESSAGE_USER_LINK_TABLE,
    primaryKeys = ["chatId", "user_id"],
    foreignKeys = [ForeignKey(
        entity = DraftMessageEntity::class,
        parentColumns = ["chatId"],
        childColumns = ["chatId"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )])
internal data class DraftMessageUserLinkEntity(
        val chatId: Long,
        @ColumnInfo(name = "user_id", index = true)
        val userId: String)