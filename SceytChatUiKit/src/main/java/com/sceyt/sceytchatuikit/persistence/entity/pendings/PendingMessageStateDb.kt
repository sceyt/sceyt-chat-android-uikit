package com.sceyt.sceytchatuikit.persistence.entity.pendings

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity

data class PendingMessageStateDb(
        @Embedded
        val entity: PendingMessageStateEntity,

        @Relation(parentColumn = "messageId", entityColumn = "message_id", entity = MessageEntity::class)
        val message: MessageDb
)