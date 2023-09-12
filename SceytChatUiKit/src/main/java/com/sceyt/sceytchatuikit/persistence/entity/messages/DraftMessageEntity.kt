package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange

@Entity
data class DraftMessageEntity(
        @PrimaryKey
        val chatId: Long,
        val message: String?,
        var createdAt: Long,
        val metadata: String?,
        val replyOrEditMessageId: Long?,
        val isReplyMessage: Boolean?,
        val styleRanges: List<BodyStyleRange>?,
)
