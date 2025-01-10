package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.BodyAttribute

@Entity
data class DraftMessageEntity(
        @PrimaryKey
        val chatId: Long,
        val message: String?,
        val createdAt: Long,
        val replyOrEditMessageId: Long?,
        val isReplyMessage: Boolean?,
        val styleRanges: List<BodyAttribute>?,
)
