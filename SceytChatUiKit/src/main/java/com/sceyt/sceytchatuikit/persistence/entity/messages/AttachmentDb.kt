package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation

data class AttachmentDb(
        @Embedded
        val attachmentEntity: AttachmentEntity,

        @Relation(parentColumn = "messageTid", entityColumn = "messageTid")
        val payLoad: AttachmentPayLoadEntity?,
)