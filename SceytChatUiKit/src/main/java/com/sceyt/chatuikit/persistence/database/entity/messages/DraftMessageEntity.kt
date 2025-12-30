package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.DRAFT_MESSAGE_TABLE

@Entity(tableName = DRAFT_MESSAGE_TABLE)
internal data class DraftMessageEntity(
    @PrimaryKey
    val chatId: Long,
    val message: String?,
    val createdAt: Long,
    val replyOrEditMessageId: Long?,
    val isReplyMessage: Boolean?,
    val styleRanges: List<BodyAttribute>?,
    @ColumnInfo(defaultValue = "0")
    val viewOnce: Boolean,
)
