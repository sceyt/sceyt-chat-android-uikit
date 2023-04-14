package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

data class DraftMessageDb(
        @Embedded
        val draftMessageEntity: DraftMessageEntity,

        @Relation(parentColumn = "chatId", entityColumn = "user_id",
            entity = UserEntity::class,
            associateBy = Junction(DraftMessageUserLink::class))
        val users: List<UserEntity>?
)