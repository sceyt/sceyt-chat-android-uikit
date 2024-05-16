package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.*
import com.sceyt.chatuikit.persistence.entity.UserEntity

data class ReactionDb(
        @Embedded
        val reaction: ReactionEntity,
        @Relation(parentColumn = "fromId", entityColumn = "user_id")
        val from: UserEntity?
)
