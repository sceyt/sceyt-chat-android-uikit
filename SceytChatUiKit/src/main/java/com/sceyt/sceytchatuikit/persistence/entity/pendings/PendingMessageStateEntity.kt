package com.sceyt.sceytchatuikit.persistence.entity.pendings

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.MessageState

@Entity(tableName = "PendingMessageState")
data class PendingMessageStateEntity(
        @PrimaryKey
        val messageId: Long,
        val channelId: Long,
        val state: MessageState,
        val editBody: String?,
        val deleteOnlyForMe: Boolean
)