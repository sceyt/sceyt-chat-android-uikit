package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sceyt.chat.models.message.MessageState

internal const val PENDING_MESSAGE_STATE_TABLE = "sceyt_pending_message_state_table"

@Entity(tableName = PENDING_MESSAGE_STATE_TABLE)
internal data class PendingMessageStateEntity(
        @PrimaryKey
        val messageId: Long,
        val channelId: Long,
        val state: MessageState,
        val editBody: String?,
        val deleteOnlyForMe: Boolean
)