package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.UserEntity

data class MarkerWithUserDb(
        @Embedded
        val entity: MarkerEntity,

        @Relation(parentColumn = "userId", entityColumn = "user_id")
        val user: UserEntity?
)