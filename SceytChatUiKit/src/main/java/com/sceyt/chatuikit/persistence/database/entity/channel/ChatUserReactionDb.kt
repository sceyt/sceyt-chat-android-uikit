package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

internal data class ChatUserReactionDb(
        @Embedded
        val reaction: ChatUserReactionEntity,

        @Relation(parentColumn = "fromId", entityColumn = "user_id", entity = UserEntity::class)
        val from: UserDb?,

        @Relation(parentColumn = "messageId", entityColumn = "message_id", entity = MessageEntity::class)
        val message: MessageDb?
)
