package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity

internal data class PendingMessageStateDb(
        @Embedded
        val entity: PendingMessageStateEntity,

        @Relation(parentColumn = "messageId", entityColumn = "message_id", entity = MessageEntity::class)
        val message: MessageDb?
)