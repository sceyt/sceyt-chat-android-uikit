package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

data class ChanelMemberDb(
        @Embedded
        val link: UserChatLinkEntity,

        @Relation(parentColumn = "user_id", entityColumn = "user_id", entity = UserEntity::class)
        val user: UserDb?,
)