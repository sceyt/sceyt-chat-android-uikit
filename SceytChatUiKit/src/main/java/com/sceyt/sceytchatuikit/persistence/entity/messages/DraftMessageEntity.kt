package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DraftMessageEntity(
        @PrimaryKey
        val chatId: Long,
        val message: String?,
        var createdAt: Long,
)