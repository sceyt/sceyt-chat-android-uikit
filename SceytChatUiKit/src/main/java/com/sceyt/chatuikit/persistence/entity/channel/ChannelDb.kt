package com.sceyt.chatuikit.persistence.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.entity.pendings.PendingReactionEntity
import com.sceyt.chatuikit.persistence.entity.user.UserDb

data class ChannelDb(
        @Embedded val channelEntity: ChannelEntity,

        @Relation(parentColumn = "chat_id", entityColumn = "chat_id", entity = UserChatLink::class)
        val members: List<ChanelMemberDb>?,

        @Relation(parentColumn = "lastMessageTid", entityColumn = "tid", entity = MessageEntity::class)
        val lastMessage: MessageDb?,

        @Relation(parentColumn = "createdById", entityColumn = "user_id", entity = UserEntity::class)
        val createdBy: UserDb?,

        @Relation(parentColumn = "chat_id", entityColumn = "channelId", entity = ChatUserReactionEntity::class)
        val newReactions: List<ChatUserReactionDb>?,

        @Relation(parentColumn = "chat_id", entityColumn = "chatId", entity = DraftMessageEntity::class)
        val draftMessage: DraftMessageDb?,

        @Relation(parentColumn = "chat_id", entityColumn = "channelId")
        val pendingReactions: List<PendingReactionEntity>?
)
