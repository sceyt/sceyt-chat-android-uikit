package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.UserEntity

data class MentionUserDb(
        @Embedded
        val link: MentionUserMessageLink,

        @Relation(parentColumn = "user_id", entityColumn = "user_id")
        val user: UserEntity?,
)
