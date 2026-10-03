package com.sceyt.chatuikit.persistence.database.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity

internal data class PinnedMessageDb(
    @Embedded
    val pinnedMessageEntity: PinnedMessageEntity,

    @Relation(parentColumn = "messageTid", entityColumn = "tid", entity = MessageEntity::class)
    val message: MessageDb?,

    @Relation(parentColumn = "pinnedByUserId", entityColumn = "user_id", entity = UserEntity::class)
    val pinnedBy: UserDb?,
)