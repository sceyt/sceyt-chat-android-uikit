package com.sceyt.sceytchatuikit.persistence.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink

data class MemberEntity(
        @Embedded
        val link: UserChatLink,

        @Relation(parentColumn = "user_id", entityColumn = "user_id")
        val user: UserEntity,
)