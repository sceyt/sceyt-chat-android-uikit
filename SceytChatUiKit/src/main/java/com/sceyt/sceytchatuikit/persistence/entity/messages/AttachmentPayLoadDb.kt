package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.sceytchatuikit.persistence.entity.link.LinkDetailsEntity

data class AttachmentPayLoadDb(
        @Embedded
        val payLoadEntity: AttachmentPayLoadEntity,

        @Relation(parentColumn = "url", entityColumn = "link")
        val linkPreviewDetails: LinkDetailsEntity?
)