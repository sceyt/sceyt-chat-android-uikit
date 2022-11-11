package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

data class ChanelMemberDb(
        @Embedded
        val link: UserChatLink,

        @Relation(parentColumn = "user_id", entityColumn = "user_id")
        val user: UserEntity,
)