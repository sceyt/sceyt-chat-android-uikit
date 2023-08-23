package com.sceyt.sceytchatuikit.persistence.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.pendings.PendingReactionEntity

data class ChannelDb(
        @Embedded val channelEntity: ChannelEntity,

        @Relation(parentColumn = "chat_id", entityColumn = "chat_id", entity = UserChatLink::class)
        val members: List<ChanelMemberDb>?,

        @Relation(parentColumn = "lastMessageTid", entityColumn = "tid", entity = MessageEntity::class)
        val lastMessage: MessageDb?,

        @Relation(parentColumn = "createdById", entityColumn = "user_id")
        val createdBy: UserEntity?,

        @Relation(parentColumn = "chat_id", entityColumn = "channelId", entity = ChatUserReactionEntity::class)
        val newReactions: List<ChatUserReactionDb>?,

        @Relation(parentColumn = "chat_id", entityColumn = "chatId", entity = DraftMessageEntity::class)
        val draftMessage: DraftMessageDb?,

        @Relation(parentColumn = "chat_id", entityColumn = "channelId")
        val pendingReactions: List<PendingReactionEntity>?
)
