package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

internal data class MessageDb(
        @Embedded
        val messageEntity: MessageEntity,

        @Relation(parentColumn = "fromId", entityColumn = "user_id", entity = UserEntity::class)
        val from: UserDb?,

        @Relation(parentColumn = "parentId", entityColumn = "message_id", entity = MessageEntity::class)
        val parent: ParentMessageDb?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = AttachmentEntity::class)
        val attachments: List<AttachmentDb>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val userMarkers: List<MarkerEntity>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId", entity = ReactionEntity::class)
        val reactions: List<ReactionDb>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val reactionsTotals: MutableList<ReactionTotalEntity>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val pendingReactions: List<PendingReactionEntity>?,

        @Relation(parentColumn = "userId", entityColumn = "user_id", entity = UserEntity::class)
        val forwardingUser: UserDb?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = MentionUserMessageLinkEntity::class)
        val mentionedUsers: List<MentionUserDb>?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = PollEntity::class)
        val poll: PollDb?,
) {
    val selfReactions get() = reactions?.filter { it.from?.id == SceytChatUIKit.chatUIFacade.myId }

    val id get() = messageEntity.id
}