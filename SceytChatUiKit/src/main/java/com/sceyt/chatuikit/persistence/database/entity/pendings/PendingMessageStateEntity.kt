package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_MESSAGE_STATE_TABLE

@Entity(tableName = PENDING_MESSAGE_STATE_TABLE)
internal data class PendingMessageStateEntity(
        @PrimaryKey
        val messageId: Long,
        val channelId: Long,
        val state: MessageState,
        val editBody: String?,
        val deleteOnlyForMe: Boolean
)