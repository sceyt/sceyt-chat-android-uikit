package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.user.UserDb
import com.sceyt.chatuikit.persistence.entity.user.UserEntity

data class MarkerWithUserDb(
        @Embedded
        val entity: MarkerEntity,

        @Relation(parentColumn = "userId", entityColumn = "user_id", entity = UserEntity::class)
        val user: UserDb?
)