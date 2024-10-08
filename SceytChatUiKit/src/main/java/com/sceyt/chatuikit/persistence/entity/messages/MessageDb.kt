package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.persistence.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.entity.pendings.PendingReactionEntity
import com.sceyt.chatuikit.persistence.entity.user.UserDb

data class MessageDb(
        @Embedded
        val messageEntity: MessageEntity,

        @Relation(parentColumn = "fromId", entityColumn = "user_id", entity = UserEntity::class)
        val from: UserDb?,

        @Relation(parentColumn = "parentId", entityColumn = "message_id", entity = MessageEntity::class)
        val parent: ParentMessageDb?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = AttachmentEntity::class)
        val attachments: List<AttachmentDb>?,

        @Relation(
            parentColumn = "message_id",
            entityColumn = "primaryKey",
            associateBy = Junction(UserMarkerLink::class, parentColumn = "message_id", entityColumn = "markerId"))
        val userMarkers: List<MarkerEntity>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId", entity = ReactionEntity::class)
        val reactions: List<ReactionDb>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val reactionsTotals: MutableList<ReactionTotalEntity>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val pendingReactions: List<PendingReactionEntity>?,

        @Relation(parentColumn = "userId", entityColumn = "user_id", entity = UserEntity::class)
        val forwardingUser: UserDb?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = MentionUserMessageLink::class)
        val mentionedUsers: List<MentionUserDb>?
) {
    val selfReactions get() = reactions?.filter { it.from?.id == SceytChatUIKit.chatUIFacade.myId }
}