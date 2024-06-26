package com.sceyt.chatuikit.persistence.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.UserEntity
import com.sceyt.chatuikit.persistence.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.entity.messages.MessageEntity

data class ChatUserReactionDb(
        @Embedded
        val reaction: ChatUserReactionEntity,

        @Relation(parentColumn = "fromId", entityColumn = "user_id")
        val from: UserEntity?,

        @Relation(parentColumn = "messageId", entityColumn = "message_id", entity = MessageEntity::class)
        val message: MessageDb?
)
