package com.sceyt.chatuikit.persistence.entity.user

import androidx.room.Embedded
import androidx.room.Relation

data class UserDb(
        @Embedded
        val user: UserEntity,

        @Relation(parentColumn = "user_id", entityColumn = "user_id")
        val metadata: List<UserMetadataEntity>
) {
    val id get() = user.id
}