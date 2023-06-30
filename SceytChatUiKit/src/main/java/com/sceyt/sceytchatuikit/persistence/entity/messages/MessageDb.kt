package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity

data class MessageDb(
        @Embedded
        val messageEntity: MessageEntity,

        @Relation(parentColumn = "fromId", entityColumn = "user_id")
        val from: UserEntity?,

        @Relation(parentColumn = "parentId", entityColumn = "message_id", entity = MessageEntity::class)
        val parent: ParentMessageDb?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = AttachmentEntity::class)
        val attachments: List<AttachmentDb>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        val userMarkers: List<MarkerEntity>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId", entity = ReactionEntity::class)
        val reactions: List<ReactionDb>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        var reactionsTotals: List<ReactionTotalEntity>?,

        @Relation(parentColumn = "message_id", entityColumn = "messageId")
        var pendingReactions: List<PendingReactionEntity>?,

        @Relation(parentColumn = "userId", entityColumn = "user_id")
        val forwardingUser: UserEntity?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = MentionUserMessageLink::class)
        val mentionedUsers: List<MentionUserDb>?
) {
    val selfReactions get() = reactions?.filter { it.from?.id == SceytKitClient.myId }
}