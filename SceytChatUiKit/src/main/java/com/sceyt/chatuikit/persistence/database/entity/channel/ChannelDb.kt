package com.sceyt.chatuikit.persistence.database.entity.channel

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity


internal data class ChannelDb(
        @Embedded val channelEntity: ChannelEntity,

        @Relation(parentColumn = "chat_id", entityColumn = "chat_id", entity = UserChatLinkEntity::class)
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
