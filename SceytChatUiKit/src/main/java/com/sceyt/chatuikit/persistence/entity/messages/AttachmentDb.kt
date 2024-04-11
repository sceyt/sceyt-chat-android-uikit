package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Embedded
import androidx.room.Relation
import com.sceyt.chatuikit.persistence.entity.link.LinkDetailsEntity

data class AttachmentDb(
        @Embedded
        val attachmentEntity: AttachmentEntity,

        @Relation(parentColumn = "messageTid", entityColumn = "messageTid")
        val payLoad: AttachmentPayLoadEntity?,

        @Relation(parentColumn = "url", entityColumn = "link")
        val linkDetails: LinkDetailsEntity?,
)