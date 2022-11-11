package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity

data class ChannelDb(
        @Embedded val channelEntity: ChannelEntity,

        @Relation(parentColumn = "chat_id", entityColumn = "chat_id", entity = UserChatLink::class)
        val members: List<ChanelMemberDb>?,

        @Relation(parentColumn = "lastMessageTid", entityColumn = "tid", entity = MessageEntity::class)
        val lastMessage: MessageDb?
)
