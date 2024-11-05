package com.sceyt.chatuikit.persistence.entity.messages

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sceyt.chatuikit.persistence.file_transfer.TransferState

@Entity(tableName = "AttachmentPayLoad",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [Index("messageTid", unique = true)])
data class AttachmentPayLoadEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val messageTid: Long,
        val transferState: TransferState,
        val progressPercent: Float?,
        val url: String?,
        val filePath: String?
)