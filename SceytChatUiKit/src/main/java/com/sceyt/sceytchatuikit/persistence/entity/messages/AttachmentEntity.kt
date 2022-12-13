package com.sceyt.sceytchatuikit.persistence.entity.messages

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["tid"],
            childColumns = ["messageTid"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ],
    indices = [
        Index(value = ["messageTid"]),
        Index(value = ["messageTid", "url"], unique = true)])
data class AttachmentEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val messageId: Long,
        val messageTid: Long,
        val tid: Long,
        val name: String,
        val type: String,
        val metadata: String?,
        val fileSize: Long,
        @ColumnInfo(index = true)
        val url: String?,
        val filePath: String?,
        val transferState: TransferState?,
        val progressPercent: Float?
)