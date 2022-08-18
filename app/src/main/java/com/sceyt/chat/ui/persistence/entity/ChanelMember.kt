package com.sceyt.chat.ui.persistence.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chat.ui.persistence.entity.channel.UserChatLink

data class ChanelMember(
        @Embedded
        val link: UserChatLink,

        @Relation(parentColumn = "user_id", entityColumn = "user_id")
        val user: UserEntity,
)