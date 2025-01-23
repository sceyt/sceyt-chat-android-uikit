package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

data class ParentMessageDb(
        @Embedded
        val messageEntity: MessageEntity,

        @Relation(parentColumn = "fromId", entityColumn = "user_id", entity = UserEntity::class)
        val from: UserDb?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = AttachmentEntity::class)
        val attachments: List<AttachmentDb>?,

        @Relation(parentColumn = "tid", entityColumn = "messageTid", entity = MentionUserMessageLink::class)
        val mentionedUsers: List<MentionUserDb>?
)