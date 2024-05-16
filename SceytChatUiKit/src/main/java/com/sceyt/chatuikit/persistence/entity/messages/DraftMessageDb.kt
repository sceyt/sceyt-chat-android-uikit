package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.UserEntity

data class DraftMessageDb(
        @Embedded
        val draftMessageEntity: DraftMessageEntity,

        @Relation(parentColumn = "chatId", entityColumn = "user_id",
            entity = UserEntity::class,
            associateBy = Junction(DraftMessageUserLink::class))
        val mentionUsers: List<UserEntity>?,

        @Relation(parentColumn = "replyOrEditMessageId", entityColumn = "message_id", entity = MessageEntity::class)
        val replyOrEditMessage: MessageDb?
)