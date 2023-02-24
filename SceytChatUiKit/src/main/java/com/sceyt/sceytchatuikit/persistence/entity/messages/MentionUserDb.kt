package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

data class MentionUserDb(
        @Embedded
        val link: MentionUserMessageLink,

        @Relation(parentColumn = "user_id", entityColumn = "user_id")
        val user: UserEntity?,
)
