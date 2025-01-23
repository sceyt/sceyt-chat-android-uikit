package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

data class MentionUserDb(
        @Embedded
        val link: MentionUserMessageLink,

        @Relation(parentColumn = "user_id", entityColumn = "user_id", entity = UserEntity::class)
        val user: UserDb?,
)
