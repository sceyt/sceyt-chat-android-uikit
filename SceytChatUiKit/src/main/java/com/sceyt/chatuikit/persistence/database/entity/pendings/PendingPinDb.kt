package com.sceyt.chatuikit.persistence.database.entity.pendings

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity

internal data class PendingPinDb(
    @Embedded
    val pendingPin: PendingPinEntity,

    @Relation(parentColumn = "messageTid", entityColumn = "tid", entity = MessageEntity::class)
    val message: MessageDb?,
)