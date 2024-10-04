package com.sceyt.chatuikit.persistence.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.user.UserDb
import com.sceyt.chatuikit.persistence.entity.user.UserEntity

data class ChanelMemberDb(
        @Embedded
        val link: UserChatLink,

        @Relation(parentColumn = "user_id", entityColumn = "user_id", entity = UserEntity::class)
        val user: UserDb?,
)