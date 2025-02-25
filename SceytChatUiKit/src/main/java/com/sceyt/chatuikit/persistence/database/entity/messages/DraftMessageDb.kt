package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

internal data class DraftMessageDb(
        @Embedded
        val draftMessageEntity: DraftMessageEntity,

        @Relation(parentColumn = "chatId", entityColumn = "user_id",
            entity = UserEntity::class,
            associateBy = Junction(DraftMessageUserLinkEntity::class), )
        val mentionUsers: List<UserDb>?,

        @Relation(parentColumn = "replyOrEditMessageId", entityColumn = "message_id", entity = MessageEntity::class)
        val replyOrEditMessage: MessageDb?
)