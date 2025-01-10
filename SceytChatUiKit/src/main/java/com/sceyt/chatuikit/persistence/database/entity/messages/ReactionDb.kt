package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.*
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

data class ReactionDb(
        @Embedded
        val reaction: ReactionEntity,
        @Relation(parentColumn = "fromId", entityColumn = "user_id", entity = UserEntity::class)
        val from: UserDb?
)