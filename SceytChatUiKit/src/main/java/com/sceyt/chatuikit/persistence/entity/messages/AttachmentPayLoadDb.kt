package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.link.LinkDetailsEntity

data class AttachmentPayLoadDb(
        @Embedded
        val payLoadEntity: AttachmentPayLoadEntity,

        @Relation(parentColumn = "url", entityColumn = "link")
        val linkPreviewDetails: LinkDetailsEntity?
)