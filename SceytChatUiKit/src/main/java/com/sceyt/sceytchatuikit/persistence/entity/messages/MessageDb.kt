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

        @Relation(parentColumn = "tid", entityColumn = "messageTid")
        val attachments: List<AttachmentEntity>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId", entity = ReactionEntity::class)
        val lastReactions: List<ReactionDb>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val reactionsScores: List<ReactionScoreEntity>?
)