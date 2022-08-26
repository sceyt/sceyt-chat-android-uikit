package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

data class MessageDb(
        @Embedded
        val messageEntity: MessageEntity,

        @Relation(parentColumn = "fromId", entityColumn = "user_id")
        val from: UserEntity?,

        @Relation(parentColumn = "parentId", entityColumn = "message_id")
        val parent: MessageEntity?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val attachments: List<AttachmentEntity>?
)